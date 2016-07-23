package org.icgc.dcc.dev.jira;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;

@Service
public class JiraService {

  /**
   * Constants.
   */
  private static final String STATUS_FIELD_NAME = "status";
  private static final String TEST_STATUS = "Ready for testing";

  /**
   * Dependencies.
   */
  @Autowired
  JiraClient jira;

  public JiraTicket getTicket(@NonNull String key) {
    val issue = getIssue(key);

    return new JiraTicket()
        .setKey(key)
        .setTitle(issue.getSummary())
        .setStatus(issue.getStatus().getName())
        .setAssignee(issue.getAssignee().getName())
        .setUrl(issue.getUrl());
  }

  @SneakyThrows
  public void updateTicket(@NonNull String key, String comment) {
    val issue = getIssue(key);
    if (!issue.getStatus().getName().equals(TEST_STATUS)) {
      issue.update().field(STATUS_FIELD_NAME, TEST_STATUS);
    }

    issue.addComment(comment);
  }

  @SneakyThrows
  private Issue getIssue(String key) {
    return jira.getIssue(key);
  }

}
