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
package org.icgc.dcc.dev.server.jenkins;

import static com.google.common.primitives.Ints.tryParse;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.JenkinsBuildsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildCause;
import com.offbytwo.jenkins.model.MavenBuild;
import com.offbytwo.jenkins.model.MavenJobWithDetails;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Jenkins façade service.
 */
@Slf4j
@Service
public class JenkinsService {

  /**
   * Constants.
   */
  static final Pattern CAUSE_SHORT_DESCRIPTION_PATTERN =
      Pattern.compile("GitHub pull request #(\\d+) of commit ([a-f0-9]+)");

  /**
   * Configuration.
   */
  @Value("${jenkins.jobName}")
  String jobName;

  /**
   * Dependencies.
   */
  @Autowired
  JenkinsServer jenkins;
  @Autowired
  MessageService messages;

  /**
   * Poll at regular intervals for available builds.
   */
  @Scheduled(cron = "${jenkins.cron}")
  @Synchronized
  public void poll() {
    log.debug("Polling...");
    messages.sendMessage(new JenkinsBuildsMessage().setBuilds(getLatestBuildsByPR()));
  }

  @SneakyThrows
  public List<JenkinsBuild> getBuilds() {
    return builds().map(this::convert).collect(toImmutableList());
  }

  @SneakyThrows
  public JenkinsBuild getBuild(@NonNull Integer buildNumber) {
    val defaultValue = new JenkinsBuild().setNumber(buildNumber);

    return builds().filter(b -> b.getNumber() == buildNumber).findFirst().map(this::convert).orElse(defaultValue);
  }

  @SneakyThrows
  public List<JenkinsBuild> getBuildsByPR(@NonNull Integer prNumber) {
    return builds().map(this::convert).filter(b -> b.getPrNumber() == prNumber).collect(toImmutableList());
  }

  @SneakyThrows
  public Optional<JenkinsBuild> getLatestBuildByPR(@NonNull Integer prNumber) {
    return getBuildsByPR(prNumber).stream().collect(latestBuild());
  }

  @SneakyThrows
  public List<JenkinsBuild> getLatestBuildsByPR() {
    return builds()
        .map(this::convert)
        .filter(b -> b.getPrNumber() != null) // Filter manual as we can't detect PR
        .collect(groupingBy(JenkinsBuild::getPrNumber, latestBuild()))
        .values().stream()
        .map(Optional::get)
        .collect(toImmutableList());
  }

  @SneakyThrows
  private MavenJobWithDetails getJob() {
    return jenkins.getMavenJob(jobName);
  }

  private Stream<MavenBuild> builds() {
    return getJob().getBuilds().stream();
  }

  @SneakyThrows
  private JenkinsBuild convert(MavenBuild build) {
    val matcher = matchCause(build.details().getCauses());
    val prNumber = matcher.isPresent() ? tryParse(matcher.get().group(1)) : null;
    val commitId = matcher.isPresent() ? matcher.get().group(2) : null;

    if (prNumber == null) {
      log.warn("PR number missing for build: {} - {}", build.getUrl(), resolveCause(build));
    }

    return new JenkinsBuild()
        .setNumber(build.getNumber())
        .setPrNumber(prNumber)
        .setCommitId(commitId)
        .setUrl(build.getUrl())
        .setResult(build.details().getResult())
        .setTimestamp(build.details().getTimestamp());
  }

  private static Optional<Matcher> matchCause(List<BuildCause> causes) {
    // Heuristic to get commit and PR
    return causes.stream()
        .map(BuildCause::getShortDescription)
        .map(CAUSE_SHORT_DESCRIPTION_PATTERN::matcher)
        .filter(Matcher::find)
        .findFirst();
  }

  private static String resolveCause(MavenBuild build) throws IOException {
    if (build == null ||
        build.details() == null ||
        build.details().getCauses() == null ||
        build.details().getCauses().isEmpty()) return null;

    return build.details().getCauses().get(0).getShortDescription();
  }

  private static Collector<JenkinsBuild, ?, Optional<JenkinsBuild>> latestBuild() {
    return maxBy(comparing(JenkinsBuild::getNumber));
  }

}
