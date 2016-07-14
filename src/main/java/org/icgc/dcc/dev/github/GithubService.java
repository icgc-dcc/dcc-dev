package org.icgc.dcc.dev.github;

import static org.kohsuke.github.GHIssueState.OPEN;

import java.util.List;

import org.kohsuke.github.GHCommitStatus;
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
  
  @SneakyThrows
  public List<String> getPrs() {
    val list = repo.queryPullRequests().state(OPEN).list();
    val prs = ImmutableList.<String>builder();
    for (val pr : list) {
      prs.add(pr.getUrl().toString());
    }
    
    return prs.build();
  }
  
  @SneakyThrows
  public GHCommitStatus getStatus(String sha1) {
    return repo.getLastCommitStatus(sha1);
  }
  
}
