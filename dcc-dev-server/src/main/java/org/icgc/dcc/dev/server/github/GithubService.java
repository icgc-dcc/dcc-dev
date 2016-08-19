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
package org.icgc.dcc.dev.server.github;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.kohsuke.github.GHIssueState.OPEN;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.GithubPrsMessage;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstraction for interacting with GitHub pull requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService {

  /**
   * Constants.
   */
  static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("/([^/]+)/?$");

  /**
   * Dependencies.
   */
  final GHRepository repo;
  final MessageService messages;

  /**
   * Poll at regular intervals for available PRs.
   */
  @Scheduled(cron = "${github.cron}")
  @Synchronized
  public void poll() {
    log.debug("Polling...");
    messages.sendMessage(new GithubPrsMessage().setPrs(getPrs()));
  }

  public Optional<GithubPr> getPr(@NonNull Integer prNumber) {
    return getPrs().stream()
        .filter(pr -> prNumber.equals(pr.getNumber()))
        .findFirst();
  }

  @SneakyThrows
  public List<GithubPr> getPrs() {
    val prs = repo.queryPullRequests().state(OPEN).list();
    return stream(prs)
        .map(this::convert)
        .collect(toImmutableList());
  }

  @SneakyThrows
  public Optional<String> getBuildNumber(@NonNull String sha1) {
    val status = repo.getLastCommitStatus(sha1);

    return Optional.ofNullable(parseBuildNumber(status.getTargetUrl()));
  }

  private GithubPr convert(GHPullRequest pr) {
    return new GithubPr()
        .setNumber(pr.getNumber())
        .setTitle(pr.getTitle())
        .setDescription(pr.getBody())
        .setUser(pr.getUser().getLogin())
        .setBranch(pr.getHead().getRef())
        .setHead(pr.getHead().getSha())
        .setAvatarUrl(pr.getUser().getAvatarUrl())
        .setUrl(pr.getHtmlUrl().toString());
  }

  private static String parseBuildNumber(String jobUrl) {
    val matcher = BUILD_NUMBER_PATTERN.matcher(jobUrl);

    return matcher.find() ? matcher.group(1) : null;
  }

}
