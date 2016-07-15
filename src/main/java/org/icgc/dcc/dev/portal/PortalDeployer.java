package org.icgc.dcc.dev.portal;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Ordering.natural;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.val;

@Component
public class PortalDeployer {

  @Value("${template.dir}")
  File templateDir;
  
  @Autowired
  PortalFileSystem fileSystem;
  
  @SneakyThrows
  public void deploy(Portal portal) {
    val id = nextId();
    portal.setId(id);
    
    val targetDir = fileSystem.getRootDir(id);
    if (!targetDir.exists()) {
      copyTemplate(targetDir);
    }
    
    donwnloadJar(portal);
  }
  
  @SneakyThrows
  public void update(Portal portal) {
    donwnloadJar(portal);
  }
  
  @SneakyThrows
  public void undeploy(String id) {
    val targetDir = fileSystem.getRootDir(id);
    
    deleteDirectory(targetDir);
  }

  private void copyTemplate(File targetDir) throws IOException {
    copyDirectory(templateDir, targetDir);
  }

  private void donwnloadJar(Portal portal) throws MalformedURLException, IOException {
    val artifactUrl = new URL(portal.getTarget().getArtifact());
    val jarFile = fileSystem.getJarFile(portal.getId());

    copy(artifactUrl.openStream(), jarFile.toPath(), REPLACE_EXISTING);
  }

  private String nextId() {
    val id = resolveIds().stream().map(Integer::valueOf).sorted(natural().reversed()).findFirst().orElse(0);
    
    return String.valueOf(id + 1);
  }

  private List<String> resolveIds() {
    return copyOf(fileSystem.getDir().list());
  }
  
}
