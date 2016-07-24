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

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dev.jira.JiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.Synchronized;
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

  public Portal get(@NonNull String portalId) {
    return repository.get(portalId);
  }

  @Synchronized
  public Portal create(@NonNull String prNumber, String name, String title, String description, String ticket,
      Map<String, String> properties) {
    log.info("Creating portal {}...", name);
    
    // Resolve portal candidate by PR
    val candidate = candidates.resolve(prNumber);
    if (candidate == null) {
      return null;
    }

    // Collect metadata in a single object
    val portal = new Portal()
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setProperties(properties)
        .setTarget(candidate);

    // Create directory with artifact
    deployer.deploy(portal);
    val url = publicUrl + "/portal/" + portal.getId();
    
    // Ensure ticket is marked for test with the portal URL
    jira.updateTicket(ticket, "Deployed to " + url +" for testing");

    // Save the metadata
    repository.save(portal);
    
    // Start the portal
    val output = executor.start(portal.getId(), portal.getProperties());
    log.info("Output: {}", output);

    // Stream log lines to UI
    logs.startTailing(portal.getId());

    return portal;
  }

  @Synchronized
  public Portal update(@NonNull String portalId, String name, String title, String description, String ticket,
      Map<String, String> properties) {
    log.info("Updating portal {}...", portalId);
    val portal = repository.get(portalId);

    repository.save(portal
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setProperties(properties));

    executor.stop(portalId);
    deployer.update(portal);
    executor.start(portalId, properties);

    return portal;
  }

  @Synchronized
  public void remove(@NonNull String portalId) {
    log.info("Removing portal {}...", portalId);
    deployer.undeploy(portalId);
    executor.stop(portalId);
    logs.stopTailing(portalId);
  }

  public void start(@NonNull String portalId) {
    log.info("Starting portal {}...", portalId);
    val portal = repository.get(portalId);
    executor.start(portalId, portal.getProperties());
    logs.startTailing(portalId);
  }

  public void restart(@NonNull String portalId) {
    log.info("Restarting portal {}...", portalId);
    val portal = repository.get(portalId);
    executor.restart(portalId, portal.getProperties());
    logs.startTailing(portalId);
  }

  public void stop(@NonNull String portalId) {
    log.info("Stopping portal {}...", portalId);
    executor.stop(portalId);
    logs.stopTailing(portalId);
  }

  public String status(@NonNull String portalId) {
    log.info("Getting status of portal {}...", portalId);
    return executor.status(portalId);
  }

}
