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
package org.icgc.dcc.dev.github;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;
import static org.kohsuke.github.GHCommitState.SUCCESS;
import static org.kohsuke.github.GHIssueState.OPEN;

import java.util.List;
import java.util.regex.Pattern;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Service
public class GithubService {

  /**
   * Constants.
   */
  private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("/([^/]+)/?$");
  
  /**
   * Dependencies.
   */
  @Autowired
  GHRepository repo;

  public GithubPr getPr(@NonNull String number) {
    return getPrs().stream().filter(pr -> number.equals(pr.getNumber())).findFirst().orElse(null);
  }

  @SneakyThrows
  public List<GithubPr> getPrs() {
    val prs = repo.queryPullRequests().state(OPEN).list();
    
    return stream(prs).map(this::convert).collect(toImmutableList());
  }

  @SneakyThrows
  public String getBuildNumber(@NonNull String sha1) {
    val status = repo.getLastCommitStatus(sha1);
    val state = status.getState();
    if (state != SUCCESS) return null;

    return parseBuildNumber(status.getTargetUrl());
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
