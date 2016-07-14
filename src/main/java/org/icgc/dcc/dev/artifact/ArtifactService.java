package org.icgc.dcc.dev.artifact;

import java.util.List;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.Searches;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.jfrog.artifactory.client.model.RepoPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.SneakyThrows;

@Service
public class ArtifactService {

  @Value("${artifact.repo}")
  String repo;
  @Value("${artifact.name}")
  String name;
  @Autowired
  Artifactory artifactory;
  
  @SneakyThrows
  public List<RepoPath> list() {
    return search().doSearch();
  }

  @SneakyThrows
  public List<RepoPath> getBuild(String buildNumber) {
     return search().itemsByProperty().property("build.number", buildNumber).doSearch();
  }
  
  @SneakyThrows
  public List<Item> folder() {
    Folder folder = artifactory.repository(repo).folder("org/icgc/dcc/dcc-portal-api").info();
    return folder.getChildren();
  }

  private Searches search() {
    return artifactory.searches().repositories(repo).artifactsByName(name);
  }
  
}
