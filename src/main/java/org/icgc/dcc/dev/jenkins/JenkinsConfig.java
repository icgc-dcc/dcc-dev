package org.icgc.dcc.dev.jenkins;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.offbytwo.jenkins.JenkinsServer;

import lombok.SneakyThrows;

@Configuration
@EnableScheduling
public class JenkinsConfig {

  @Bean
  @SneakyThrows
  public JenkinsServer jenkins(@Value("${jenkins.url}") URI url, @Value("${jenkins.user}") String user, @Value("${jenkins.password}") String password) {
    return new JenkinsServer(url, user, password);
  }

}
