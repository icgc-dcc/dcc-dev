package org.icgc.dcc.dev.jira;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import lombok.SneakyThrows;
import lombok.val;
import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.JiraClient;

@Service
public class JiraConfig {

  @Bean
  @SneakyThrows
  public JiraClient jira(
      @Value("${jira.url}") URI url, 
      @Value("${jira.user}") String user,
      @Value("${jira.password}") String password) {
    val creds = new BasicCredentials(user, password);
    return new JiraClient(url.toString(), creds);    
  }
  
}
