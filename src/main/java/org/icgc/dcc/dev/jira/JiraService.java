package org.icgc.dcc.dev.jira;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import net.rcarz.jiraclient.JiraClient;

@Service
public class JiraService {

  /**
   * Dependencies.
   */
  @Autowired
  JiraClient jira;

  @SneakyThrows
  public JiraTicket getTicket(@NonNull String key) {
    val issue = jira.getIssue(key);

    return new JiraTicket()
        .setKey(key)
        .setTitle(issue.getSummary())
        .setStatus(issue.getStatus().getName())
        .setAssignee(issue.getAssignee().getName())
        .setUrl(issue.getUrl());
  }

}
