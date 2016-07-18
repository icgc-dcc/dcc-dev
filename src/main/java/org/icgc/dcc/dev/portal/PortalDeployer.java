package org.icgc.dcc.dev.portal;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Ordering.natural;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

import com.google.common.collect.ImmutableSet;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PortalDeployer {

  @Value("${template.dir}")
  File templateDir;
  @Value("${template.url}")
  URL templateUrl;
  @Value("${template.settings}")
  File templateSettings;

  @Autowired
  PortalFileSystem fileSystem;
  
  @SneakyThrows
  public void setup() {
    log.info("Creating template...");
    templateDir.mkdirs();

    @Cleanup
    val tar = new TarArchiveInputStream(new GZIPInputStream(templateUrl.openStream()));

    ArchiveEntry tarEntry;
    while ((tarEntry = tar.getNextEntry()) != null) {
      // Strip staring directory with version from path
      val name = tarEntry.getName().replaceFirst("^[^/]+/", "");
      val file = new File(templateDir, name);
      if (tarEntry.isDirectory()) {
        checkState(file.mkdirs(), "Could not make dir %s", file);
        continue;
      }

      val dir = file.getParentFile();
      if (!dir.exists()) {
        checkState(dir.mkdirs(), "Could not make dir %s", dir);
      }

      log.info("Extracting {}...", file);
      try (val output = new FileOutputStream(file)) {
        copy(tar, output);
      }
    }
  }

  @SneakyThrows
  public void deploy(Portal portal) {
    val id = nextId();
    portal.setId(id);

    val targetDir = fileSystem.getRootDir(id);
    if (!targetDir.exists()) {
      copyTemplate(targetDir);

      // Make executable
      val binaries = fileSystem.getBinDir(id).listFiles();
      for (val binary : binaries) {
        Files.setPosixFilePermissions(binary.toPath(), ImmutableSet.of(OWNER_EXECUTE, OWNER_READ));
      }
    }

    val settingsFile = fileSystem.getSettingsFile(id);
    
    log.info("Copying settings from {} to {}", templateSettings, settingsFile);
    copy(templateSettings.toPath(), settingsFile.toPath(), REPLACE_EXISTING);

    // TODO: Override port settings
    val httpPort = SocketUtils.findAvailableTcpPort(8000, 9000);
    val adminPort = SocketUtils.findAvailableTcpPort(httpPort, 9000);
    log.info("httpPort = {}, adminPort = {}", httpPort, adminPort);
    
    downloadJar(portal);
  }

  @SneakyThrows
  public void update(Portal portal) {
    downloadJar(portal);
  }

  @SneakyThrows
  public void undeploy(String id) {
    val targetDir = fileSystem.getRootDir(id);

    deleteDirectory(targetDir);
  }

  private void copyTemplate(File targetDir) throws IOException {
    copyDirectory(templateDir, targetDir);
  }

  private void downloadJar(Portal portal) throws MalformedURLException, IOException {
    val artifactUrl = new URL(portal.getTarget().getArtifact());
    val jarFile = fileSystem.getJarFile(portal.getId());

    log.info("Downloading {} to {}", artifactUrl, jarFile);
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
