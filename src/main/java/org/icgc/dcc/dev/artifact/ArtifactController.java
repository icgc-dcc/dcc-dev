package org.icgc.dcc.dev.artifact;

import java.util.List;

import org.jfrog.artifactory.client.model.RepoPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class ArtifactController {

  @Autowired
  ArtifactService artifacts;
  
  
  @RequestMapping("/builds")
  public List<RepoPath> builds() {
    return artifacts.list();
  }
  
}
