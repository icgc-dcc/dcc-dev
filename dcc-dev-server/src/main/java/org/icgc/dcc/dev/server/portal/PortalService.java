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
import static org.icgc.dcc.dev.server.portal.PortalUpdates.newConfig;
import static org.icgc.dcc.dev.server.portal.PortalUpdates.newDescription;
import static org.icgc.dcc.dev.server.portal.PortalUpdates.newSlug;
import static org.icgc.dcc.dev.server.portal.PortalUpdates.newTicketKey;
import static org.icgc.dcc.dev.server.portal.PortalUpdates.newTitle;
import static org.icgc.dcc.dev.server.portal.util.Portals.WEB_BASE_URL_PROPERTY;
import static org.icgc.dcc.dev.server.portal.util.Portals.getServerPort;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.icgc.dcc.dev.server.github.GithubService;
import org.icgc.dcc.dev.server.jira.JiraService;
import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.PortalChangeMessage;
import org.icgc.dcc.dev.server.message.Messages.PortalChangeType;
import org.icgc.dcc.dev.server.portal.candidate.PortalCandidates;
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
  PortalCandidates candidates;
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
  @Autowired
  GithubService github;

  public List<Portal.Candidate> getCandidates() {
    return candidates.getCandidates();
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
      Map<String, String> config, boolean autoDeploy, boolean autoRefresh, boolean autoRemove, String username,
      boolean start) {
    log.info("{}", repeat("-", 80));
    log.info("Creating portal for PR {}...", prNumber);
    log.info("{}", repeat("-", 80));

    // Validate
    validateSlug(slug, null);

    // Resolve portal candidate by PR
    val candidate = candidates.getCandidate(prNumber).orElseThrow(() -> new PortalPrNotFoundException(prNumber));

    // Collect metadata in a single object
    Portal portal = new Portal()
        .setTitle(newTitle(title, null, candidate.getPr().getTitle()))
        .setSlug(newSlug(slug, null, title, null, candidate.getPr().getTitle()))
        .setDescription(newDescription(description, null, candidate.getPr().getDescription()))
        .setTicketKey(newTicketKey(ticket, null, candidate.getTicket()))
        .setConfig(newConfig(config, null))
        .setAutoDeploy(autoDeploy)
        .setAutoRefresh(autoRefresh)
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

    portal = deploy(portal);

    if (start) {
      // Start the portal
      start(portal.getId());

      // Ensure PR / ticket is marked for test with the portal URL
      updatePr(portal);
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
      try {
        executor.stop(portal);
      } catch (Exception e) {
        log.warn("Problem stopping portal: {}", e.getMessage());
      }
    }

    portal = deploy(portal);

    executor.startAsync(portal);

    notifyChange(portal, PortalChangeType.UPDATED);
  }

  public Portal update(@NonNull Integer portalId, String slug, String title, String description, String ticket,
      Map<String, String> config, boolean autoDeploy, boolean autoRefresh, boolean autoRemove) {
    log.info("Updating portal {}...", portalId);

    // Validate
    validateSlug(slug, portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    Portal portal = get(portalId);

    val candidate = portal.getTarget();
    portal
        .setTitle(newTitle(title, portal.getTitle(), candidate.getPr().getTitle()))
        .setSlug(newSlug(slug, portal.getSlug(), title, portal.getTitle(), candidate.getPr().getTitle()))
        .setDescription(newDescription(description, portal.getDescription(), candidate.getPr().getDescription()))
        .setTicketKey(newTicketKey(ticket, portal.getTicketKey(), candidate.getTicket()))
        .setConfig(newConfig(config, portal.getConfig()))
        .setAutoDeploy(autoDeploy)
        .setAutoRefresh(autoRefresh)
        .setAutoRemove(autoRemove);

    executor.stop(portal);

    portal = deploy(portal);

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
    try {
      executor.stop(portal);
    } catch (Exception e) {
      log.warn("Problem stopping portal: {}", e.getMessage());
    }

    // Remove physical directory
    deployer.undeploy(portalId);

    // Remove metadata
    repository.delete(portalId);

    notifyChange(portal, PortalChangeType.REMOVED);
  }

  public void start(@NonNull Integer portalId) {
    execute("Starting", portalId, executor::startAsync);
  }

  public void restart(@NonNull Integer portalId) {
    execute("Restarting", portalId, executor::restartAsync);
  }

  public void stop(@NonNull Integer portalId) {
    execute("Stopping", portalId, executor::stopAsync);
  }

  public String getLog(Integer portalId) {
    log.info("Getting log of portal {}...", portalId);
    if (!repository.exists(portalId)) throw new PortalNotFoundException(portalId);

    @Cleanup
    val lock = locks.lockReading(portalId);

    return logs.cat(portalId);
  }

  private Portal deploy(Portal portal) {
    // Install jar
    deployer.deploy(portal);

    // Assign URL
    portal.setUrl(resolvePublicUrl(portal));
    portal.getSystemConfig().put(WEB_BASE_URL_PROPERTY, resolveInternalUrl(publicUrl, portal));

    // Update
    return repository.save(portal);
  }

  private void execute(String message, @NonNull Integer portalId, Consumer<Portal> action) {
    log.info("{} portal {}...", message, portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    action.accept(portal);
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
      log.info("Updating JIRA {} for portal {}...", ticketKey, portal.getId());
      jira.updateTicket(ticketKey, formatMessage(portal), false);
    } catch (Exception e) {
      log.error("Could not update ticket " + ticketKey + ":", e);
    }
  }

  private void updatePr(Portal portal) {
    val prNumber = portal.getTarget().getPr().getNumber();

    try {
      log.info("Updating PR {} for portal {}...", prNumber, portal.getId());
      github.addComment(prNumber, formatMessage(portal));
    } catch (Exception e) {
      log.error("Could not add comment to PR " + prNumber + ":", e);
    }
  }

  private String formatMessage(Portal portal) {
    val iframeUrl = resolvePublicUrl(portal);
    return "Deployed to " + iframeUrl + " for testing";
  }

  private void notifyChange(Portal portal, PortalChangeType type) {
    messages.sendMessage(new PortalChangeMessage()
        .setType(type)
        .setPortalId(portal.getId()));
  }

  private String resolvePublicUrl(Portal portal) {
    return publicUrl + "/portals/" + portal.getId();
  }

  private static String resolveInternalUrl(URL publicUrl, Portal portal) {
    // Replace this port and add portal port
    return UriComponentsBuilder
        .fromHttpUrl(publicUrl.toString())
        .port(getServerPort(portal))
        .toUriString();
  }

}
