package org.icgc.dcc.dev.portal;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PortalFileSystem {

  @Value("${workspace.dir}")
  File workspaceDir;

  public File getDir() {
    return new File(workspaceDir, "portals");
  }

  public File getRootDir(String portalId) {
    return new File(getDir(), portalId);
  }

  public File getBinDir(String portalId) {
    return new File(getRootDir(portalId), "bin");
  }

  public File getSettingsFile(String portalId) {
    return new File(getConfDir(portalId), "application.yml");
  }

  public File getConfDir(String portalId) {
    return new File(getRootDir(portalId), "conf");
  }

  public File getLibDir(String portalId) {
    return new File(getRootDir(portalId), "lib");
  }

  public File getLogsDir(String portalId) {
    return new File(getRootDir(portalId), "logs");
  }

  public File getScriptFile(String portalId) {
    return new File(getBinDir(portalId), "dcc-portal-server");
  }

  public File getMetadataFile(String portalId) {
    return new File(getRootDir(portalId), "portal.json");
  }

  public File getJarFile(String portalId) {
    return new File(getLibDir(portalId), "dcc-portal-server.jar");
  }

  public File getLogFile(String portalId) {
    return new File(getLibDir(portalId), "dcc-portal-server.log");
  }

}
