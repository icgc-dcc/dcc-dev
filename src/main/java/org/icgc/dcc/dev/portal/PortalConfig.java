package org.icgc.dcc.dev.portal;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PortalConfig {

  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${template.dir}")
  File templateDir;
  
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalService service;

  @PostConstruct
  @SneakyThrows
  public void init() {
    if (!workspaceDir.exists()) {
      log.info("Creating workspace...");
      checkState(workspaceDir.mkdirs(), "Could not create workspace dir %s", workspaceDir);
    }

    if (!templateDir.exists()) {
      log.info("Creating template...");
      deployer.setup();
    }
    
    val portals = service.list();
    for (val portal : portals) {
      log.info("Restaring portal {}...", portal.getId());
      service.restart(portal.getId());
    }
  }

}
