package org.icgc.dcc.dev.portal;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dev.log.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PortalService {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;

  /**
   * Dependencies.
   */
  @Autowired
  PortalRepository repository;
  @Autowired
  LogService logs;
  @Autowired
  PortalFileSystem fileSystem;
  @Autowired
  PortalDeployer deployer;
  @Autowired
  PortalExecutor executor;
  @Autowired
  PortalCandidateResolver candidates;

  public List<Portal.Candidate> getCandidates() {
    return candidates.resolve();
  }

  public List<Portal> list() {
    return repository.list();
  }

  public Portal get(String portalId) {
    return repository.get(portalId);
  }

  @Synchronized
  public Portal create(String prNumber, String name, String title, String description, String ticket,
      Map<String, String> properties) {
    log.info("Creating portal {}...", name);
    val candidate = candidates.resolve(prNumber);
    if (candidate == null) {
      return null;
    }

    // Collect metadata
    val portal = new Portal()
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setProperties(properties)
        .setTarget(candidate);

    // Create directory with aritfact
    deployer.deploy(portal);

    // Save the metadata
    repository.save(portal);
    val output = executor.start(portal.getId(), portal.getProperties());
    log.info("Output: {}", output);

    logs.startTailing(portal.getId());

    return portal;
  }

  @Synchronized
  public Portal update(String portalId, String name, String title, String description, String ticket,
      Map<String, String> properties) {
    log.info("Updating portal {}...", portalId);
    val portal = repository.get(portalId);

    repository.save(portal
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setProperties(properties));

    executor.stop(portalId);
    deployer.update(portal);
    executor.start(portalId, properties);

    return portal;
  }

  @Synchronized
  public void remove(String portalId) {
    log.info("Removing portal {}...", portalId);
    deployer.undeploy(portalId);
    executor.stop(portalId);
    logs.stopTailing(portalId);
  }

  public void start(String portalId) {
    log.info("Starting portal {}...", portalId);
    val portal = repository.get(portalId);
    executor.start(portalId, portal.getProperties());
    logs.startTailing(portalId);
  }

  public void restart(String portalId) {
    log.info("Restarting portal {}...", portalId);
    val portal = repository.get(portalId);
    executor.restart(portalId, portal.getProperties());
    logs.startTailing(portalId);
  }

  public void stop(String portalId) {
    log.info("Stopping portal {}...", portalId);
    executor.stop(portalId);
    logs.stopTailing(portalId);
  }

  public String status(String portalId) {
    log.info("Getting status of portal {}...", portalId);
    return executor.status(portalId);
  }

}
