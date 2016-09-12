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
import static com.google.common.primitives.Ints.compare;

import java.util.List;

import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.message.Messages.GithubPrsMessage;
import org.icgc.dcc.dev.server.message.Messages.JenkinsBuildsMessage;
import org.icgc.dcc.dev.server.portal.PortalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.google.common.collect.Multimaps;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener that handles upstream portal candidate events.
 */
@Slf4j
@Component
public class PortalCandidateListener {

  @Autowired
  PortalService portals;

  /**
   * Listens for new build events and determines if a portal update is required.
   * 
   * @param message the current list of builds.
   */
  @EventListener
  public void handle(@NonNull JenkinsBuildsMessage message) {
    val prBuilds = Multimaps.index(message.getBuilds(), JenkinsBuild::getPrNumber);

    for (val portal : portals.list()) {
      val candidate = portal.getTarget();
      val prNumber = candidate.getPr().getNumber();

      // Resolve the latest build for the current portal
      val portalBuilds = prBuilds.get(prNumber);
      val latestBuild = findLatestBuild(portalBuilds);

      val currentBuild = candidate.getBuild();
      val deployed = currentBuild != null;
      if (deployed) {
        // No need to update if we are the latest already
        val buildLatest = latestBuild.getNumber() <= currentBuild.getNumber();
        if (buildLatest) continue;

        log.debug("Build update found for portal {}:  {}", portal.getId(), latestBuild);

        // No need to update if we are in manual mode
        if (!portal.isAutoRefresh()) continue;

        log.info("Auto deploying portal {}: {}", portal.getId(), latestBuild);
        candidate.setBuild(latestBuild);
      } else {
        // No need to create if we are in manual mode
        if (!portal.isAutoDeploy()) continue;

        log.info("First build found for portal {}:  {}", portal.getId(), latestBuild);
        candidate.setBuild(latestBuild);
      }

      portals.update(portal);
    }
  }

  private JenkinsBuild findLatestBuild(List<JenkinsBuild> portalBuilds) {
    // Sort decending by build number
    return portalBuilds.stream()
        .sorted((b1, b2) -> -compare(b1.getNumber(), b2.getNumber()))
        .findFirst().orElse(null);
  }

  @EventListener
  public void handle(@NonNull GithubPrsMessage message) {
    val openPrNumbers = uniqueIndex(message.getPrs(), GithubPr::getNumber);

    for (val portal : portals.list()) {
      val prNumber = portal.getTarget().getPr().getNumber();
      val prOpen = openPrNumbers.containsKey(prNumber);
      if (prOpen) continue;

      log.debug("Closed PR found for portal {}", portal.getId());
      if (!portal.isAutoRemove()) continue;

      log.info("Auto removing portal {}", portal.getId());
      portals.remove(portal.getId());
    }
  }

}
