package org.icgc.dcc.dev.portal;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;

@Component
public class PortalFileSystem {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${artifact.artifactId}")
  String baseName = "dcc-portal-server";

  public File getDir() {
    return new File(workspaceDir, "portals");
  }

  public File getRootDir(@NonNull String portalId) {
    return new File(getDir(), portalId);
  }

  public File getBinDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "bin");
  }

  public File getSettingsFile(@NonNull String portalId) {
    return new File(getConfDir(portalId), "application.yml");
  }

  public File getConfDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "conf");
  }

  public File getLibDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "lib");
  }

  public File getLogsDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "logs");
  }

  public File getScriptFile(@NonNull String portalId) {
    return new File(getBinDir(portalId), baseName);
  }

  public File getMetadataFile(@NonNull String portalId) {
    return new File(getRootDir(portalId), "portal.json");
  }

  public File getJarFile(@NonNull String portalId) {
    return new File(getLibDir(portalId), baseName + ".jar");
  }

  public File getLogFile(@NonNull String portalId) {
    return new File(getLibDir(portalId), baseName + ".log");
  }

}
