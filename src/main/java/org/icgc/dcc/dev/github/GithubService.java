package org.icgc.dcc.dev.github;

import static org.kohsuke.github.GHIssueState.OPEN;

import java.util.List;

import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;
import lombok.val;

@Service
public class GithubService {

  @Autowired
  GHRepository repo;

  public GithubPr getPr(String number) {
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
  public String getBuildNumber(String sha1) {
    val status = repo.getLastCommitStatus(sha1);
    val jobUrl = status.getTargetUrl();
    
    return jobUrl.split("/")[5];
  }

  private GithubPr convert(GHPullRequest element) {
    return new GithubPr()
        .setNumber(element.getNumber() + "")
        .setTitle(element.getTitle())
        .setDescription(element.getBody())
        .setUser(element.getUser().getLogin())
        .setBranch(element.getHead().getRef())
        .setHead(element.getHead().getSha())
        .setAvatarUrl(element.getUser().getAvatarUrl())
        .setUrl(element.getHtmlUrl().toString());
  }

}
