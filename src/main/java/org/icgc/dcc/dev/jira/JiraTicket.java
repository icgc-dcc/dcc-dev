package org.icgc.dcc.dev.jira;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class JiraTicket {

  /**
   * Issue key.
   * <p>
   * Primary key.
   */
  String key;
  
  String title;
  String status;
  String assignee;
  String url;

}
