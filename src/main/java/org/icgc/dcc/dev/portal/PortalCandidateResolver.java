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

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.regex.Pattern;

import org.icgc.dcc.dev.artifactory.ArtifactoryService;
import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.github.GithubService;
import org.icgc.dcc.dev.jenkins.JenkinsService;
import org.icgc.dcc.dev.jira.JiraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;

@Component
public class PortalCandidateResolver {

  /**
   * Constants.
   */
  private static final Pattern PR_PATTERN = Pattern.compile("^(DCC-\\d+)/.*$");

  /**
   * Dependencies.
   */
  @Autowired
  JiraService jira;
  @Autowired
  GithubService github;
  @Autowired
  JenkinsService jenkins;
  @Autowired
  ArtifactoryService artifactory;

  public List<Portal.Candidate> resolve() {
    return github.getPrs().stream().map(this::resolve).collect(toList());
  }

  public Portal.Candidate resolve(@NonNull Integer prNumber) {
    val pr = github.getPr(prNumber);

    return resolve(pr);
  }

  public Portal.Candidate resolve(@NonNull GithubPr pr) {
    val buildNumber = github.getBuildNumber(pr.getHead());
    if (buildNumber == null) return null;

    val build = jenkins.getBuild(buildNumber);
    val artifact = artifactory.getArtifact(buildNumber);
    val ticketKey = parseTicketKey(pr.getBranch());
    val ticket = ticketKey == null ? null : jira.getTicket(ticketKey);

    return new Portal.Candidate()
        .setPr(pr)
        .setBuild(build)
        .setArtifact(artifact.orElse(null))
        .setTicket(ticket);
  }

  private static String parseTicketKey(String branch) {
    val matcher = PR_PATTERN.matcher(branch);

    return matcher.find() ? matcher.group(1) : null;
  }

}
