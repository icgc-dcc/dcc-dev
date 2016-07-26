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
package org.icgc.dcc.dev.portal;

import static org.springframework.util.SocketUtils.findAvailableTcpPort;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dev.jira.JiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PortalService {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${server.publicUrl}")
  URL publicUrl;

  /**
   * Dependencies.
   */
  @Autowired
  PortalRepository repository;
  @Autowired
  PortalLogService logs;
  @Autowired
  PortalFileSystem fileSystem;
  @Autowired
  JiraService jira;
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalExecutor executor;
  @Autowired
  PortalCandidateResolver candidates;

  public List<Portal.Candidate> getCandidates() {
    return candidates.resolve();
  }

  public List<Portal> list() {
    return repository.list();
  }

  public Portal get(@PortalLock @NonNull String portalId) {
    return repository.get(portalId);
  }

  public Portal create(@NonNull Integer prNumber, String name, String title, String description, String ticket,
      Map<String, String> properties) {
    log.info("Creating portal {}...", name);

    // Resolve portal candidate by PR
    val candidate = candidates.resolve(prNumber);
    if (candidate == null) return null;

    // Collect metadata in a single object
    val portal = new Portal()
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setProperties(properties)
        .setTarget(candidate);

    // Create id and directory with artifact
    deployer.deploy(portal);
    repository.save(portal);

    // Assign ports and URL
    assignPorts(portal);
    portal.setUrl(resolveUrl(portal));
    repository.save(portal);

    // Start the portal
    start(portal.getId());

    // Ensure ticket is marked for test with the portal URL
    updateTicket(portal);

    return portal;
  }

  public Portal update(@PortalLock(write = true) @NonNull String portalId, String name, String title, String description, String ticket,
      Map<String, String> properties) {
    log.info("Updating portal {}...", portalId);
    val portal = repository.get(portalId);

    repository.save(portal
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setProperties(properties));

    executor.stop(portal);
    deployer.update(portal);
    executor.start(portal);

    return portal;
  }

  public void remove(@PortalLock(write = true) @NonNull String portalId) {
    log.info("Removing portal {}...", portalId);
    val portal = repository.get(portalId);

    executor.stop(portal);
    logs.stopTailing(portalId);
    
    deployer.undeploy(portalId);
  }

  public void start(@PortalLock(write = true) @NonNull String portalId) {
    log.info("Starting portal {}...", portalId);
    val portal = repository.get(portalId);

    executor.start(portal);
    logs.startTailing(portalId);
  }

  public void restart(@PortalLock(write = true) @NonNull String portalId) {
    log.info("Restarting portal {}...", portalId);
    val portal = repository.get(portalId);

    executor.restart(portal);
    logs.startTailing(portalId);
  }

  public void stop(@PortalLock(write = true) @NonNull String portalId) {
    log.info("Stopping portal {}...", portalId);
    val portal = repository.get(portalId);

    executor.stop(portal);
    logs.stopTailing(portalId);
  }

  public String status(@PortalLock @NonNull String portalId) {
    log.info("Getting status of portal {}...", portalId);
    return executor.status(portalId);
  }
  
  public String getLog(@PortalLock @NonNull String portalId) {
    log.info("Getting log of portal {}...", portalId);
    return logs.cat(portalId);
  }

  private String resolveUrl(Portal portal) {
    // Strip this port and add portal port
    return publicUrl.toString().replaceFirst(":\\d+", "") + ":" + portal.getSystemProperties().get("server.port");
  }

  private void updateTicket(Portal portal) {
    val ticketKey = portal.getTicket() != null ? portal
        .getTicket() : portal.getTarget().getTicket() != null ? portal.getTarget().getTicket().getKey() : null;
    if (ticketKey == null) return;

    val iframeUrl = publicUrl + "/" + portal.getId();
    jira.updateTicket(ticketKey, "Deployed to " + iframeUrl + " for testing");
  }

  private static void assignPorts(Portal portal) {
    val systemProperties = portal.getSystemProperties();
    systemProperties.put("server.port", findFreePort());
    systemProperties.put("management.port", findFreePort());
    log.info("systemProperties: {}", systemProperties);
  }

  private static String findFreePort() {
    return String.valueOf(findAvailableTcpPort(8000, 9000));
  }

}
