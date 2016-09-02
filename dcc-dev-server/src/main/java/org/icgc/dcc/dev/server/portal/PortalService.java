/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.dev.server.portal;

import static com.google.api.client.repackaged.com.google.common.base.Strings.repeat;
import static org.icgc.dcc.dev.server.portal.util.Portals.getServerPort;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dev.server.jira.JiraService;
import org.icgc.dcc.dev.server.jira.JiraTicket;
import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.PortalChangeMessage;
import org.icgc.dcc.dev.server.message.Messages.PortalChangeType;
import org.icgc.dcc.dev.server.portal.candidate.PortalCandidateResolver;
import org.icgc.dcc.dev.server.portal.io.PortalDeployer;
import org.icgc.dcc.dev.server.portal.io.PortalExecutor;
import org.icgc.dcc.dev.server.portal.io.PortalFileSystem;
import org.icgc.dcc.dev.server.portal.io.PortalLogs;
import org.icgc.dcc.dev.server.portal.util.PortalLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.slugify.Slugify;
import com.google.common.collect.ImmutableList;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Main service responsible for coordinating the life cycle management of portal instances.
 * <p>
 * One of the main aspects this service provides is locking semantics.
 */
@Slf4j
@Service
public class PortalService {

  /**
   * Configuration.
   */
  @Value("${server.publicUrl}")
  URL publicUrl;

  /**
   * Dependencies.
   */
  @Autowired
  PortalCandidateResolver candidates;
  @Autowired
  PortalRepository repository;
  @Autowired
  PortalFileSystem fileSystem;
  @Autowired
  PortalLogs logs;
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalExecutor executor;
  @Autowired
  PortalLocks locks;
  @Autowired
  MessageService messages;
  @Autowired
  JiraService jira;

  public List<Portal.Candidate> getCandidates() {
    return candidates.resolve();
  }

  public Portal get(@NonNull Integer portalId) {
    val portal = repository.findOne(portalId);
    if (portal == null) throw new PortalNotFoundException(portalId);

    return portal;
  }

  public Portal getBySlug(@NonNull String slug) {
    return repository.findBySlug(slug).orElseThrow(() -> new PortalNotFoundException(slug));
  }

  public Portal.Status getStatus(@NonNull Integer portalId) {
    @Cleanup
    val lock = locks.lockReading(portalId);
    if (!repository.exists(portalId)) throw new PortalNotFoundException(portalId);

    return executor.getStatus(portalId);
  }

  public List<Portal> list() {
    return ImmutableList.copyOf(repository.findAll());
  }

  public Portal create(@NonNull Integer prNumber, String slug, String title, String description, String ticket,
      Map<String, String> config, boolean autoDeploy, boolean autoRemove, String username, boolean start) {
    log.info("{}", repeat("-", 80));
    log.info("Creating portal for PR {}...", prNumber);
    log.info("{}", repeat("-", 80));

    // Validate
    validateSlug(slug, null);

    // Resolve portal candidate by PR
    val candidate = candidates.resolve(prNumber).orElseThrow(() -> new PortalPrNotFoundException(prNumber));

    // Collect metadata in a single object
    Portal portal = new Portal()
        .setTitle(resolveTitle(title, null, candidate.getPr().getTitle()))
        .setSlug(resolveSlug(slug, null, title, null, candidate.getPr().getTitle()))
        .setDescription(resolveDescription(description, null, candidate.getPr().getDescription()))
        .setTicketKey(resolveTicketKey(ticket, null, candidate.getTicket()))
        .setConfig(resolveConfig(config, null))
        .setAutoDeploy(autoDeploy)
        .setAutoRemove(autoRemove)
        .setUsername(username)
        .setTarget(candidate);

    // Save instance
    portal = repository.save(portal);

    // Lock
    @Cleanup
    val lock = locks.lockWriting(portal);

    // Create directory
    deployer.init(portal);

    // Install jar
    deployer.deploy(portal);

    // Assign URL
    portal.setUrl(resolveUrl(publicUrl, portal));
    repository.save(portal);

    if (start) {
      // Start the portal
      start(portal.getId());

      // Ensure ticket is marked for test with the portal URL
      updateTicket(portal);
    }

    notifyChange(portal, PortalChangeType.CREATED);

    return portal;
  }

  public void update(Portal portal) {
    @Cleanup
    val lock = locks.lockWriting(portal);

    val status = getStatus(portal.getId());
    if (status.isRunning()) {
      executor.stop(portal);
    }

    deployer.deploy(portal);
    repository.save(portal);

    executor.startAsync(portal);

    notifyChange(portal, PortalChangeType.UPDATED);
  }

  public Portal update(@NonNull Integer portalId, String slug, String title, String description, String ticket,
      Map<String, String> config, boolean autoDeploy, boolean autoRemove) {
    log.info("Updating portal {}...", portalId);

    // Validate
    validateSlug(slug, portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    Portal portal = get(portalId);

    val candidate = portal.getTarget();
    portal
        .setTitle(resolveTitle(title, portal.getTitle(), candidate.getPr().getTitle()))
        .setSlug(resolveSlug(slug, portal.getSlug(), title, portal.getTitle(), candidate.getPr().getTitle()))
        .setDescription(resolveDescription(description, portal.getDescription(), candidate.getPr().getDescription()))
        .setTicketKey(resolveTicketKey(ticket, portal.getTicketKey(), candidate.getTicket()))
        .setConfig(resolveConfig(config, portal.getConfig()))
        .setAutoDeploy(autoDeploy)
        .setAutoRemove(autoRemove);

    executor.stop(portal);
    
    deployer.deploy(portal);
    portal = repository.save(portal);

    executor.startAsync(portal);

    notifyChange(portal, PortalChangeType.UPDATED);

    return portal;
  }

  public void remove() {
    log.info("**** Removing all portals!");
    for (val portal : list()) {
      remove(portal.getId());
    }
  }

  public void remove(@NonNull Integer portalId) {
    log.info("Removing portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    // Wait for the instance to stop (synchronous)
    executor.stop(portal);
    deployer.undeploy(portalId);

    // Remove meatdata
    repository.delete(portalId);

    notifyChange(portal, PortalChangeType.REMOVED);
  }

  public void start(@NonNull Integer portalId) {
    log.info("Starting portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.startAsync(portal);
  }

  public void restart(@NonNull Integer portalId) {
    log.info("Restarting portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.restartAsync(portal);
  }

  public void stop(@NonNull Integer portalId) {
    log.info("Stopping portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.stopAsync(portal);
  }

  public String getLog(Integer portalId) {
    log.info("Getting log of portal {}...", portalId);
    // TODO: Do existence check instead
    get(portalId);

    @Cleanup
    val lock = locks.lockReading(portalId);

    return logs.cat(portalId);
  }

  @SneakyThrows
  private void validateSlug(String slug, Integer portalId) {
    if (slug == null) return;

    val empty = slug.trim().equals("");
    if (empty) throw new PortalValidationException("Portal slug '%s' cannot be blank", slug);

    val slugifiedSlug = new Slugify().slugify(slug);
    val slugified = slug.equals(slugifiedSlug);
    if (!slugified) throw new PortalValidationException("Portal slug '%s' is not slugified. Should be '%s'",
        slug, slugifiedSlug);

    val existingPortal = repository.findBySlug(slug);
    val duplicate = existingPortal.isPresent() && !existingPortal.get().getId().equals(portalId);
    if (duplicate) throw new PortalValidationException("Portal %s already exists with slug '%s'",
        existingPortal.get().getId(), slug);
  }

  private void updateTicket(Portal portal) {
    val ticketKey = portal.getTicketKey();
    if (ticketKey == null) return;

    try {
      val iframeUrl = publicUrl + "/portals/" + portal.getId();
      jira.updateTicket(ticketKey, "Deployed to " + iframeUrl + " for testing");
    } catch (Exception e) {
      log.error("Could not update ticket " + ticketKey + ":", e);
    }
  }

  private void notifyChange(Portal portal, PortalChangeType type) {
    messages.sendMessage(new PortalChangeMessage()
        .setType(type)
        .setPortalId(portal.getId()));
  }

  @SneakyThrows
  private static String resolveSlug(String newSlug, String currentSlug, String newTitle, String currentTitle,
      String prTitle) {
    return new Slugify().slugify(resolveValue(newSlug, currentSlug, prTitle));
  }

  private static String resolveTitle(String newTitle, String currentTitle, String prTitle) {
    return resolveValue(newTitle, currentTitle, prTitle);
  }

  private static String resolveDescription(String newDescription, String currentDescription, String prDescription) {
    return resolveValue(newDescription, currentDescription, prDescription);
  }

  private static String resolveTicketKey(String newTicketKey, String currentTicketKey, JiraTicket currentTicket) {
    return resolveValue(newTicketKey, currentTicketKey, currentTicket != null ? currentTicket.getKey() : null);
  }

  private static Map<String, String> resolveConfig(Map<String, String> newConfig, Map<String, String> currentConfig) {
    return resolveValue(newConfig, currentConfig);
  }

  private static String resolveUrl(URL publicUrl, Portal portal) {
    // Replace this port and add portal port
    return UriComponentsBuilder
        .fromHttpUrl(publicUrl.toString())
        .port(getServerPort(portal))
        .toUriString();
  }

  @SafeVarargs
  private static <T> T resolveValue(T... values) {
    for (T value : values)
      if (value != null) return value;

    return null;
  }

}
