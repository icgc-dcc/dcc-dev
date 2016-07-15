package org.icgc.dcc.dev.portal;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.icgc.dcc.dev.artifact.ArtifactoryService;
import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.github.GithubService;
import org.icgc.dcc.dev.jenkins.JenkinsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.val;

@Component
public class PortalCandidateResolver {

  @Autowired
  GithubService github;
  @Autowired
  ArtifactoryService artifactory;
  @Autowired
  JenkinsService jenkins;
  
  public List<Portal.Candidate> resolve() {
    return github.getPrs().stream().map(this::resolve).collect(toList());
  }
  
  public Portal.Candidate resolve(String prNumber) {
    val pr = github.getPr(prNumber);
    
    return resolve(pr);
  }
  
  public Portal.Candidate resolve(GithubPr pr) {
    val buildNumber = github.getBuildNumber(pr.getHead());
    val build = jenkins.getBuild(buildNumber);
    val artifact = artifactory.getArtifact(buildNumber);

    return new Portal.Candidate()
        .setPr(pr)
        .setBuild(build)
        .setArtifact(artifact);
  }
  
}
