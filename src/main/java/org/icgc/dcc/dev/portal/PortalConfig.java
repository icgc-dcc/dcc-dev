package org.icgc.dcc.dev.portal;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class PortalConfig {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${template.dir}")
  File templateDir;

  /**
   * Dependencies.
   */
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalService service;

  @PostConstruct
  public void init() {
    if (!workspaceDir.exists()) {
      log.info("Creating workspace...");
      checkState(workspaceDir.mkdirs(), "Could not create workspace dir %s", workspaceDir);
    }

    if (!templateDir.exists()) {
      log.info("Creating template...");
      deployer.setup();
    }
  }

  @Async
  @EventListener
  public void start(ApplicationReadyEvent event) {
    log.info("**** Started!");

    val portals = service.list();
    if (portals.isEmpty()) {
      return;
    }

    log.info("Restarting portals...");
    for (val portal : portals) {
      service.restart(portal.getId());
    }
  }

}
