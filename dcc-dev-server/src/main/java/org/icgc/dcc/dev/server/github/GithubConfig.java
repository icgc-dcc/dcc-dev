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

import java.io.File;
import java.io.IOException;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.HttpConnector;
import org.kohsuke.github.extras.OkHttpConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * GitHub module configuration.
 */
@Slf4j
@EnableScheduling
@Configuration
public class GithubConfig {

  /**
   * Configuration.
   */
  @Value("${github.cache.dir}")
  File cacheDir;

  @Bean
  @SneakyThrows
  public GHRepository repo(GitHub github, @Value("${github.repoName}") String repoName) {
    log.info("Getting repository...");
    val repo = github.getRepository(repoName);
    log.info("Finished getting repository: {}", repo);

    return repo;
  }

  @Bean
  public GitHub github(@Value("${github.user}") String user, @Value("${github.token}") String token)
      throws IOException {
    log.info("Connecting to GitHub...");
    val github = new GitHubBuilder()
        .withOAuthToken(token, user)
        .withConnector(connector())
        .build();
    log.info("Connected with rate limit: {}", github.getRateLimit());

    return github;
  }

  @Bean
  public HttpConnector connector() throws IOException {
    val maxSize = 10 * 1024 * 1024; // 10MB cache
    val cache = new Cache(cacheDir, maxSize);

    return new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache)));
  }

}
