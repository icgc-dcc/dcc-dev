package org.icgc.dcc.dev.artifact;

import java.util.List;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.model.RepoPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArtifactService {

  @Autowired
  Artifactory artifactory;
  
  public List<RepoPath> list() {
    return artifactory.searches().repositories("dcc-snapshot").artifactsByName("dcc-portal-api").doSearch();
  }
  
}
