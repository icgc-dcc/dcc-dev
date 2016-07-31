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

import static com.google.common.collect.Iterables.getLast;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.jira.JiraService;
import org.icgc.dcc.dev.server.message.Messages.GithubPrsMessage;
import org.icgc.dcc.dev.server.message.Messages.JenkinsBuildsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Main abstraction responsible for coordinating the lifecycle management of portal instances.
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
  PortalLocks locks;
  @Autowired
  PortalRepository repository;
  @Autowired
  PortalFileSystem fileSystem;
  @Autowired
  PortalLogService logs;
  @Autowired
  JiraService jira;
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalExecutor executor;
  @Autowired
  PortalCandidateResolver candidates;

  @EventListener
  public void handle(JenkinsBuildsMessage message) {
    val prBuilds = Multimaps.index(message.getBuilds(), JenkinsBuild::getPrNumber);

    for (val portal : list()) {
      val portalBuilds = prBuilds.get(portal.getTarget().getPr().getNumber());

      val latestBuild = getLast(portalBuilds);
      if (latestBuild.getNumber() <= portal.getTarget().getBuild().getNumber()) continue;

      log.info("Build update found for portal {}:  {}", portal.getId(), latestBuild);
      if (portal.isAutoDeploy()) {
        log.info("Auto deploying portal {}: {}", portal.getId(), latestBuild);
        portal.getTarget().setBuild(latestBuild);

        update(portal);
      }
    }
  }

  @EventListener
  public void handle(GithubPrsMessage message) {
    val openPrNumbers = Maps.uniqueIndex(message.getPrs(), GithubPr::getNumber);

    for (val portal : list()) {
      boolean prOpen = openPrNumbers.containsKey(portal.getTarget().getPr().getNumber());
      if (prOpen) continue;

      log.info("Closed PR found for portal {}", portal.getId());
      if (portal.isAutoRemove()) {
        log.info("Auto removing portal {}", portal.getId());
        remove(portal.getId());
      }
    }
  }

  public List<Portal.Candidate> getCandidates() {
    return candidates.resolve();
  }

  public List<Portal> list() {
    return repository.getIds().stream()
        .map(this::find)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toImmutableList());
  }

  public Portal get(@NonNull Integer portalId) {
    return find(portalId).orElseThrow(() -> new PortalNotFoundException(portalId));
  }

  public Optional<Portal> find(@NonNull Integer portalId) {
    @Cleanup
    val lock = locks.lockReading(portalId);
    return repository.get(portalId);
  }

  public Portal create(@NonNull Integer prNumber, String name, String title, String description, String ticket,
      Map<String, String> config, boolean start) {
    log.info("Creating portal for PR {}...", prNumber);

    // Resolve portal candidate by PR
    val candidate = candidates.resolve(prNumber).orElseThrow(() -> new PortalPrNotFoundException(prNumber));

    // Get new id and lock
    val portalId = deployer.nextPortalId();
    @Cleanup
    val lock = locks.lockWriting(portalId);

    // Collect metadata in a single object
    val portal = new Portal()
        .setId(portalId)
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setConfig(config)
        .setTarget(candidate);

    // Create directory
    deployer.init(portal);

    // Install jar
    deployer.deploy(portal);

    // Save instance
    repository.create(portal);

    // Assign ports and URL
    assignPorts(portal);
    portal.setUrl(resolveUrl(portal));
    repository.update(portal);

    if (start) {
      // Start the portal
      start(portal.getId());

      // Ensure ticket is marked for test with the portal URL
      updateTicket(portal);
    }

    return portal;
  }

  public void update(Portal portal) {
    @Cleanup
    val lock = locks.lockWriting(portal.getId());

    executor.stop(portal);
    deployer.deploy(portal);
    executor.start(portal);

    repository.update(portal);
  }

  public Portal update(@NonNull Integer portalId, String name, String title, String description, String ticket,
      Map<String, String> config) {
    log.info("Updating portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    repository.update(portal
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setConfig(config));

    executor.stop(portal);
    deployer.deploy(portal);
    executor.start(portal);

    return portal;
  }

  public void remove(@NonNull Integer portalId) {
    log.info("Removing portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.stop(portal);

    deployer.undeploy(portalId);
  }

  public void start(@NonNull Integer portalId) {
    log.info("Starting portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.start(portal);
  }

  public void restart(@NonNull Integer portalId) {
    log.info("Restarting portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.restart(portal);
  }

  public void stop(@NonNull Integer portalId) {
    log.info("Stopping portal {}...", portalId);

    @Cleanup
    val lock = locks.lockWriting(portalId);
    val portal = get(portalId);

    executor.stop(portal);
  }

  public String status(@NonNull Integer portalId) {
    log.info("Getting status of portal {}...", portalId);

    @Cleanup
    val lock = locks.lockReading(portalId);
    return executor.status(portalId);
  }

  public String getLog(Integer portalId) {
    log.info("Getting log of portal {}...", portalId);

    @Cleanup
    val lock = locks.lockReading(portalId);
    return logs.cat(portalId);
  }

  private void updateTicket(Portal portal) {
    val ticketKey = resolveTicketKey(portal);
    if (ticketKey == null) return;

    val iframeUrl = publicUrl + "/" + portal.getId();
    jira.updateTicket(ticketKey, "Deployed to " + iframeUrl + " for testing");
  }

  private String resolveTicketKey(Portal portal) {
    val ticket = portal.getTarget().getTicket();
    return portal.getTicket() != null ? portal.getTicket() : ticket != null ? ticket.getKey() : null;
  }

  private String resolveUrl(Portal portal) {
    // Strip this port and add portal port
    return publicUrl.toString().replaceFirst(":\\d+", "") + ":" + portal.getSystemConfig().get("server.port");
  }

  private static void assignPorts(Portal portal) {
    val systemConfig = portal.getConfig();
    systemConfig.put("server.port", findFreePort());
    systemConfig.put("management.port", findFreePort());
    log.info("systemConfig: {}", systemConfig);
  }

  private static String findFreePort() {
    return String.valueOf(findAvailableTcpPort(8000, 9000));
  }

}
