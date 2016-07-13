package org.icgc.dcc.dev.github;

import java.io.File;
import java.io.IOException;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.OkHttpConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class GithubConfig {

  @Bean
  @SneakyThrows
  public GHRepository repo(GitHub github) {
    log.info("Getting repository...");
    val repo = github.getRepository("icgc-dcc/dcc-portal");
    log.info("Finished getting repository: {}", repo);
    
    return repo;
  }

  @Bean
  public GitHub github(@Value("${github.user}") String user, @Value("${github.token}") String  token) throws IOException {
    log.info("Connecting to GitHub...");
    val github = new GitHubBuilder()
        .withOAuthToken(token, user)
        .withConnector(connector())
        .build();
    log.info("Connected: {}", github);
    
    return github;
  }

  @Bean
  public OkHttpConnector connector() throws IOException {
    val cache = new Cache(new File("/tmp/dcc-dev/responses"), 10 * 1024 * 1024); // 10MB cache
    
    return new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache)));
  }
  
}
