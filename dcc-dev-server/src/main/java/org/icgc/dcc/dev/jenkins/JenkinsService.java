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
package org.icgc.dcc.dev.jenkins;

import static com.google.common.primitives.Ints.tryParse;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.icgc.dcc.dev.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.common.primitives.Ints;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildCause;
import com.offbytwo.jenkins.model.MavenBuild;
import com.offbytwo.jenkins.model.MavenJobWithDetails;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JenkinsService {

  /**
   * Constants.
   */
  private static final Pattern CAUSE_SHORT_DESCRIPTION_PATTERN =
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
   * State.
   */
  JenkinsBuild latestBuild;

  @Scheduled(cron = "${jenkins.cron}")
  public void poll() {
    log.info("Polling...");
    val build = convert(getJob().getLastStableBuild());

    val refresh = latestBuild == null || latestBuild.getNumber() < build.getNumber();
    if (refresh) {
      latestBuild = build;

      val notify = latestBuild != null;
      if (notify) {
        log.info("New build: {}", build);
        messages.sendMessage(build);
      }
    }
  }

  @SneakyThrows
  public List<JenkinsBuild> getBuilds() {
    return builds().map(this::convert).collect(toList());
  }

  @SneakyThrows
  public JenkinsBuild getBuild(@NonNull String buildNumber) {
    val value = Ints.tryParse(buildNumber);
    val defaultValue = new JenkinsBuild().setNumber(value);

    return builds().filter(b -> b.getNumber() == value).findFirst().map(this::convert).orElse(defaultValue);
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
    val prNumber= matcher.isPresent() ? tryParse(matcher.get().group(1)) : null;
    val commitId = matcher.isPresent() ? matcher.get().group(2) : null;

    return new JenkinsBuild()
        .setNumber(build.getNumber())
        .setPrNumber(prNumber)
        .setCommitId(commitId)
        .setUrl(build.getUrl())
        .setTimestamp(build.details().getTimestamp());
  }

  private Optional<Matcher> matchCause(List<BuildCause> causes) {
    // Heuristic to get commit and pr
    return causes.stream()
        .map(cause -> CAUSE_SHORT_DESCRIPTION_PATTERN.matcher(cause.getShortDescription()))
        .filter(Matcher::find)
        .findFirst();
  }

}
