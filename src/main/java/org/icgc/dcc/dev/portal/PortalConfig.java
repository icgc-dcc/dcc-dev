package org.icgc.dcc.dev.portal;

import java.io.File;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PortalConfig {

  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${template.dir}")
  File templateDir;
  @Value("${template.url}")
  URL templateUrl;
  
  @PostConstruct
  public void init() {
     if (!workspaceDir.exists()) {
       log.info("Creating workspace...");
       workspaceDir.mkdirs();
     }
     if (!templateDir.exists()) {
       log.info("Creating template...");
       templateDir.mkdirs();
       
       // TODO: Download from templateUrl and extract to templateDir
     }
  }
  
}
