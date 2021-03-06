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

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.icgc.dcc.dev.server.artifactory.ArtifactoryService;
import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.github.GithubService;
import org.icgc.dcc.dev.server.jenkins.JenkinsService;
import org.icgc.dcc.dev.server.jira.JiraService;
import org.icgc.dcc.dev.server.jira.JiraTicket;
import org.icgc.dcc.dev.server.portal.Portal;
import org.icgc.dcc.dev.server.portal.Portal.Candidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.val;

/**
 * Responsible for finding potential portal instances.
 */
@Component
public class PortalCandidates {

  /**
   * Constants.
   */
  static final Pattern PR_TICKET_PATTERN = Pattern.compile("(DCC-\\d+|DCCPRTL-\\d+)", CASE_INSENSITIVE);

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

  public List<Portal.Candidate> getCandidates() {
    return github.getPrs().stream()
        .map(this::getCandidate)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  public Optional<Candidate> getCandidate(@NonNull Integer prNumber) {
    return github.getPr(prNumber)
        .flatMap(this::getCandidate);
  }

  public Optional<Portal.Candidate> getCandidate(@NonNull GithubPr pr) {
    val buildNumber = github.getLatestBuildNumber(pr.getHead()).orElse(null);

    return Optional.ofNullable(createCandidate(pr, buildNumber));
  }

  private Candidate createCandidate(GithubPr pr, Integer buildNumber) {
    val build = buildNumber == null ? null : jenkins.getBuild(buildNumber);
    val artifact = buildNumber == null ? null : artifactory.getArtifact(buildNumber).orElse(null);
    val ticketKey = parseTicketKey(pr.getBranch());
    val ticket = ticketKey == null ? Optional.<JiraTicket>empty() : jira.getTicket(ticketKey);

    return new Portal.Candidate()
        .setPr(pr)
        .setBuild(build)
        .setArtifact(artifact)
        .setTicket(ticket.orElse(null));
  }

  private static String parseTicketKey(String branch) {
    val matcher = PR_TICKET_PATTERN.matcher(branch);

    return matcher.find() ? matcher.group(1).toUpperCase() : null;
  }

}
