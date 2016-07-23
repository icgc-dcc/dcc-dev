package org.icgc.dcc.dev.portal;

import java.util.Map;

import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.jira.JiraTicket;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Portal {

  /**
   * The unique identifier for the portal instance.
   * <p>
   * Primary key.
   */
  String id;

  /**
   * A symbolic name for the portal instance.
   */
  String name;
  
  /**
   * A short title for the portal instance.
   */
  String title;
  
  /**
   * A longer description for the portal instance.
   */
  String description;
  
  /**
   * The JIRA ticket number for the portal instance.
   */
  String ticket;
  
  /**
   * User supplied configuration properties.
   */
  Map<String, String> properties;
  
  /**
   * System supplied configuration properties.
   * <p>
   * Not intended to be exposed to users as may contain sensitive information (e.g. credentials).
   */
  @JsonIgnore
  Map<String, String> systemProperties;

  /**
   * The absolute URL of the running portal instance.
   */
  String url;
  
  /**
   * The execution state of the portal instance.
   */
  State state = State.NONE;

  /**
   * The upstream canidate information about the running portal instance.
   */
  Candidate target;

  @Data
  @Accessors(chain = true)
  public static class Candidate {

    GithubPr pr;
    JenkinsBuild build;
    String artifact;
    JiraTicket ticket;

  }

  public static enum State {

    NONE,
    STARTED,
    STOPPED;

  }

}
