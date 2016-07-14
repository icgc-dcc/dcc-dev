package org.icgc.dcc.dev.portal;

import java.util.List;

import org.icgc.dcc.dev.artifact.ArtifactService;
import org.icgc.dcc.dev.deploy.DeployService;
import org.icgc.dcc.dev.github.GithubService;
import org.icgc.dcc.dev.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.jenkins.JenkinsService;
import org.icgc.dcc.dev.metadata.Metadata;
import org.icgc.dcc.dev.metadata.MetadataService;
import org.jfrog.artifactory.client.model.Item;
import org.jfrog.artifactory.client.model.RepoPath;
import org.kohsuke.github.GHCommitStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;

@Component
@RestController
public class PortalController {

  @Autowired
  MetadataService metadata;
  @Autowired
  DeployService deploy;
  @Autowired
  GithubService github;
  @Autowired
  ArtifactService artifacts;
  @Autowired
  JenkinsService jenkins;

  @RequestMapping("/portals")
  public List<Metadata> portals() {
    return ImmutableList.of();
  }

  @RequestMapping("/deploy")
  public String deploy() {
    return deploy.echo();
  }

  @RequestMapping("/artifacts")
  public List<RepoPath> artifacts() {
    return artifacts.list();
  }
  
  @RequestMapping("/artifacts/{buildNumber}")
  public List<RepoPath> artifacts(@PathVariable String buildNumber) {
    return artifacts.getBuild(buildNumber);
  }
  
  @RequestMapping("/folder")
  public List<Item> folder() {
    return artifacts.folder();
  }

  @RequestMapping("/prs")
  public List<String> prs() {
    return github.getPrs();
  }
  
  @RequestMapping("/status/{sha1}")
  public GHCommitStatus status(@PathVariable String sha1) {
    return github.getStatus(sha1);
  }
  
  @RequestMapping("/builds")
  public List<JenkinsBuild> builds() {
    return jenkins.getBuilds();
  }

}
