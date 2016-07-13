package org.icgc.dcc.dev.metadata;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class MetadataConfig {

  @Bean
  public ObjectMapper mapper() {
    return new ObjectMapper();
  }
  
}
