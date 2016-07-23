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
import lombok.val;

@Service
public class ArtifactoryService {

  private static final String BUILD_NUMBER_PROPERTY_NAME = "build.number";

  @Value("${artifact.repoName}")
  String repoName;
  @Value("${artifact.groupId}")
  String groupId;
  @Value("${artifact.artifactId}")
  String artifactId;

  @Autowired
  Artifactory artifactory;

  @SneakyThrows
  public List<RepoPath> list() {
    return prepareSearch().doSearch();
  }

  @SneakyThrows
  public String getArtifact(String buildNumber) {
    val paths = prepareSearch().itemsByProperty().property(BUILD_NUMBER_PROPERTY_NAME, buildNumber).doSearch();
    val path = paths.stream().filter(p -> p.getItemPath().contains(artifactId) && p.getItemPath().endsWith(".jar"))
        .findFirst().map(RepoPath::getItemPath).orElse(null);

    return artifactory.getUri() + "/" + artifactory.getContextName() + "/" + repoName + "/" + path;
  }

  @SneakyThrows
  public List<Item> getArtifactFolder() {
    val path = resolveGroupPath(groupId) + "/" + artifactId;
    Folder folder = artifactory.repository(repoName).folder(path).info();

    return folder.getChildren();
  }

  private Searches prepareSearch() {
    return artifactory.searches().repositories(repoName).artifactsByName(artifactId);
  }

  private static String resolveGroupPath(String groupId) {
    return groupId.replaceAll("\\.", "/");
  }

}
