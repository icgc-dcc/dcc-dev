package org.icgc.dcc.dev.test;

import java.util.List;

import org.icgc.dcc.dev.artifact.ArtifactoryService;
import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.github.GithubService;
import org.icgc.dcc.dev.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.jenkins.JenkinsService;
import org.jfrog.artifactory.client.model.Item;
import org.jfrog.artifactory.client.model.RepoPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class TestController {

  @Autowired
  GithubService github;
  @Autowired
  ArtifactoryService artifacts;
  @Autowired
  JenkinsService jenkins;

  @RequestMapping("/artifacts")
  public List<RepoPath> artifacts() {
    return artifacts.list();
  }

  @RequestMapping("/artifacts/{buildNumber}")
  public String artifact(@PathVariable String buildNumber) {
    return artifacts.getArtifact(buildNumber);
  }

  @RequestMapping("/folder")
  public List<Item> folder() {
    return artifacts.getArtifactFolder();
  }

  @RequestMapping("/prs")
  public List<GithubPr> prs() {
    return github.getPrs();
  }

  @RequestMapping("/builds")
  public List<JenkinsBuild> builds() {
    return jenkins.getBuilds();
  }

}
