package org.icgc.dcc.dev.portal;

import java.io.File;
import java.util.List;

import org.icgc.dcc.dev.log.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.Synchronized;
import lombok.val;

@Service
public class PortalService {

  @Value("${workspace.dir}")
  File workspaceDir;

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

  public Portal get(String id) {
    return repository.get(id);
  }

  @Synchronized
  public Portal create(String prNumber, String name, String title, String description, String ticket) {
    // Collect metadata
    val portal = new Portal()
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket)
        .setTarget(candidates.resolve(prNumber));

    // Create directory with aritfact
    deployer.deploy(portal);

    // Save the metadata
    repository.save(portal);
    executor.start(portal.getId());
    logs.tail(fileSystem.getLogFile(portal.getId()));

    return portal;
  }

  @Synchronized
  public Portal update(String id, String name, String title, String description, String ticket) {
    val portal = repository.get(id);

    repository.save(portal
        .setName(name)
        .setTitle(title)
        .setDescription(description)
        .setTicket(ticket));

    deployer.update(portal);

    return portal;
  }

  @Synchronized
  public void remove(String id) {
    stop(id);
    deployer.undeploy(id);
  }

  public void start(String id) {
    executor.start(id);
  }

  public void stop(String id) {
    executor.stop(id);
  }

  public void restart(String id) {
    executor.restart(id);
  }

}
