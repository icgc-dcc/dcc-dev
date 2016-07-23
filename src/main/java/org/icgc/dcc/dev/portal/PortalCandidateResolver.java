package org.icgc.dcc.dev.portal;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.regex.Pattern;

import org.icgc.dcc.dev.artifact.ArtifactoryService;
import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.github.GithubService;
import org.icgc.dcc.dev.jenkins.JenkinsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.val;

@Component
public class PortalCandidateResolver {

  /**
   * Constants.
   */
  private static final Pattern PR_PATTERN = Pattern.compile("^(DCC-\\d+)/.*$");

  /**
   * Dependencies.
   */
  @Autowired
  GithubService github;
  @Autowired
  JenkinsService jenkins;
  @Autowired
  ArtifactoryService artifactory;

  public List<Portal.Candidate> resolve() {
    return github.getPrs().stream().map(this::resolve).collect(toList());
  }

  public Portal.Candidate resolve(String prNumber) {
    val pr = github.getPr(prNumber);

    return resolve(pr);
  }

  public Portal.Candidate resolve(GithubPr pr) {
    val buildNumber = github.getBuildNumber(pr.getHead());
    if (buildNumber == null) return null;

    val build = jenkins.getBuild(buildNumber);
    val artifact = artifactory.getArtifact(buildNumber);
    val ticket = resolveTicket(pr.getBranch());

    return new Portal.Candidate()
        .setPr(pr)
        .setBuild(build)
        .setArtifact(artifact)
        .setTicket(ticket);
  }

  private static String resolveTicket(String branch) {
    val matcher = PR_PATTERN.matcher(branch);

    return matcher.find() ? matcher.group() : null;
  }

}
