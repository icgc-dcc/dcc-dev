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

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.GithubPrsMessage;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

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
  static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("/([^/]+)/?$"); // [url]/[buildNumber]

  /**
   * Configuration.
   */
  @Value("${github.update}")
  boolean update;

  /**
   * Dependencies.
   */
  final GHRepository repo;
  final MessageService messages;

  /**
   * Poll at regular intervals for available PRs.
   */
  @Synchronized
  @Scheduled(cron = "${github.cron}")
  public void poll() {
    log.debug("Polling...");
    messages.sendMessage(new GithubPrsMessage().setPrs(getPrs()));
  }

  @SneakyThrows
  public Optional<GithubCommit> getCommit(@NonNull String commitId) {
    try {
      val commit = repo.getCommit(commitId);

      val info = commit.getCommitShortInfo();
      return Optional.of(new GithubCommit()
          .setUser(info.getAuthor().getName())
          .setCreated(commit.getLastStatus().getCreatedAt().getTime())
          .setMessage(info.getMessage()));
    } catch (FileNotFoundException e) {
      return Optional.empty();
    }
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
  public Optional<Integer> getLatestBuildNumber(@NonNull String sha1) {
    // TODO: Search backwards through parent commits of sha1
    val statuses = repo.listCommitStatuses(sha1);
    for (val status : statuses) {
      if (isSuccessBuildStatus(status)) {
        return Optional.ofNullable(parseBuildNumber(status.getTargetUrl()));
      }
    }

    return Optional.empty();
  }
  
  public boolean isSuccessBuildStatus(GHCommitStatus status) {
    if (status.getDescription() == null) return false;

    val success = status.getState() == GHCommitState.SUCCESS;
    val finished = status.getDescription().contains("Build finished");
    return success && finished;
  }

  @SneakyThrows
  public void addComment(@NonNull Integer prNumber, @NonNull String message) {
    if (!update) {
      log.debug("Updates disabled. Skipping update of PR {}", prNumber);
      return;
    }

    val pr = repo.getPullRequest(prNumber);
    pr.comment(message);
  }

  private GithubPr convert(GHPullRequest pr) {
    // val builds = getBuilds(pr);
    
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
  
  @SneakyThrows
  private  List<Integer> getBuilds(GHPullRequest pr) {
    val builds = ImmutableList.<Integer>builder();
    for( val commit : pr.listCommits()) {
      for (val status : repo.listCommitStatuses(commit.getSha())) {
        if (isSuccessBuildStatus(status)) {
          val buildNumber = parseBuildNumber(status.getTargetUrl());
          builds.add(buildNumber);
        }
      }
    }
    
    return builds.build();
  }

  private static Integer parseBuildNumber(String jobUrl) {
    val matcher = BUILD_NUMBER_PATTERN.matcher(jobUrl);

    return matcher.find() ? Ints.tryParse(matcher.group(1)) : null;
  }

}
