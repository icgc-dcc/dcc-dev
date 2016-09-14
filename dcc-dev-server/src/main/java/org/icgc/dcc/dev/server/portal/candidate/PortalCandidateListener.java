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
package org.icgc.dcc.dev.server.portal.candidate;

import static com.google.common.collect.Maps.uniqueIndex;

import java.util.Map;

import org.icgc.dcc.dev.server.artifactory.ArtifactoryService;
import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.message.Messages.GithubPrsMessage;
import org.icgc.dcc.dev.server.message.Messages.JenkinsBuildsMessage;
import org.icgc.dcc.dev.server.portal.Portal;
import org.icgc.dcc.dev.server.portal.PortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.offbytwo.jenkins.model.BuildResult;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener that handles upstream portal candidate events.
 * <p>
 * Responsible for ensuring that builds / PRs are synchronized with portal instances based on deployer preferences.
 */
@Slf4j
@Component
public class PortalCandidateListener {

  @Autowired
  PortalService portals;
  @Autowired
  ArtifactoryService artifactory;

  /**
   * Listens for PR events and determines if a portal removal is required.
   * 
   * @param message the current list of builds.
   */
  @EventListener
  public void handle(@NonNull GithubPrsMessage message) {
    val openPrNumbers = uniqueIndex(message.getPrs(), GithubPr::getNumber);

    for (val portal : portals.list()) {
      val prNumber = portal.getTarget().getPr().getNumber();
      val prOpen = openPrNumbers.containsKey(prNumber);

      // If still open we let it live
      if (prOpen) continue;

      // Closed so consider a removal
      log.debug("Closed PR found for portal {}", portal.getId());
      if (!portal.isAutoRemove()) continue;

      // We know it needs to die
      log.info("Auto removing portal {}", portal.getId());
      portals.remove(portal.getId());
    }
  }

  /**
   * Listens for new build events and determines if a portal update is required.
   * 
   * @param message the current list of builds.
   */
  @EventListener
  public void handle(@NonNull JenkinsBuildsMessage message) {
    val prBuilds = uniqueIndex(message.getBuilds(), JenkinsBuild::getPrNumber);

    for (val portal : portals.list()) {
      handle(prBuilds, portal);
    }
  }

  private void handle(Map<Integer, JenkinsBuild> prBuilds, Portal portal) {
    val candidate = portal.getTarget();
    val prNumber = candidate.getPr().getNumber();
    val currentBuild = candidate.getBuild();

    // Resolve the latest build for the current portal
    val latestBuild = prBuilds.get(prNumber);
    
    // Skip builds that failed or in progress
    if (!isBuildReady(latestBuild)) return;

    val deployed = currentBuild != null;
    if (deployed) {
      // No need to update if we are the latest already
      if (isBuildCurrent(currentBuild, latestBuild)) return;
      
      log.debug("Build update found for portal {}:  {}", portal.getId(), latestBuild);

      // Are in manual mode?
      if (!portal.isAutoRefresh()) return;

      // Auto refresh the instance
      log.info("Auto refreshing portal {}: {}", portal.getId(), latestBuild);
    } else {
      // Are we are in manual mode?
      if (!portal.isAutoDeploy()) return;

      // Auto create the instance
      log.info("First build found for portal {}:  {}", portal.getId(), latestBuild);
    }

    val artifact = artifactory.getArtifact(latestBuild.getNumber()).orElse(null);
    if (artifact == null) {
      log.warn("Could not find artifact for portal {} and build {} ", portal.getId(), latestBuild.getNumber());
    }

    // Update portal to reflect the newly associated build
    candidate.setBuild(latestBuild);
    candidate.setArtifact(artifact);

    portals.update(portal);
  }

  private static boolean isBuildReady(JenkinsBuild latestBuild) {
    return latestBuild.getResult() == BuildResult.SUCCESS;
  }

  private static boolean isBuildCurrent(JenkinsBuild currentBuild, JenkinsBuild latestBuild) {
    return latestBuild.getNumber() <= currentBuild.getNumber();
  }

}
