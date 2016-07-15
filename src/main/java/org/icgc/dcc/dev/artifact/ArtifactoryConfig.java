package org.icgc.dcc.dev.artifact;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArtifactoryConfig {

  @Bean
  public Artifactory artifactory(@Value("${artifact.url}") String url) {
    return ArtifactoryClient.create(url);
  }
  
}
