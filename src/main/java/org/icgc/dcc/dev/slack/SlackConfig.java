package org.icgc.dcc.dev.slack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.ashwanthkumar.slack.webhook.Slack;

@Configuration
public class SlackConfig {

  @Bean
  public Slack slack(@Value("${slack.url}") String url) {
    return new Slack(url);
  }
  
}
