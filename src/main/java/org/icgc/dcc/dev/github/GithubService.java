package org.icgc.dcc.dev.github;

import static org.kohsuke.github.GHIssueState.OPEN;

import java.util.List;

import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Service
public class GithubService {

  /**
   * Dependencies.
   */
  @Autowired
  GHRepository repo;

  public GithubPr getPr(@NonNull String number) {
    return getPrs().stream().filter(pr -> number.equals(pr.getNumber())).findFirst().orElse(null);
  }

  @SneakyThrows
  public List<GithubPr> getPrs() {
    val list = repo.queryPullRequests().state(OPEN).list();
    val prs = ImmutableList.<GithubPr> builder();
    for (val element : list) {
      val pr = convert(element);
      prs.add(pr);
    }

    return prs.build();
  }

  @SneakyThrows
  public String getBuildNumber(@NonNull String sha1) {
    val status = repo.getLastCommitStatus(sha1);
    val state = status.getState();
    if (state != GHCommitState.SUCCESS) {
      return null;
    }

    val jobUrl = status.getTargetUrl();
    return jobUrl.split("/")[5];
  }

  private GithubPr convert(GHPullRequest pr) {
    return new GithubPr()
        .setNumber(pr.getNumber() + "")
        .setTitle(pr.getTitle())
        .setDescription(pr.getBody())
        .setUser(pr.getUser().getLogin())
        .setBranch(pr.getHead().getRef())
        .setHead(pr.getHead().getSha())
        .setAvatarUrl(pr.getUser().getAvatarUrl())
        .setUrl(pr.getHtmlUrl().toString());
  }

}
