package org.icgc.dcc.dev.portal;

import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.jenkins.JenkinsBuild;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Portal {

  String id;
  
  String name;
  String title;
  String description;
  String ticket;

  String url;
  State state = State.NONE;
  
  Candidate target;
  
  @Data
  @Accessors(chain = true)
  public static class Candidate {

    GithubPr pr;
    JenkinsBuild build;
    String artifact;
    
  }
  
  public static enum State {
    
    NONE,
    STARTED,
    STOPPED;
    
  }
  
}
