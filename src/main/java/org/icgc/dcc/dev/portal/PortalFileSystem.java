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

  public File getRootDir(String id) {
    return new File(getDir(), id);
  }
  
  public File getBinDir(String id) {
    return new File(getRootDir(id), "bin");
  }

  public File getSettingsFile(String id) {
    return new File(getConfDir(id), "settings.yml");
  }
  
  public File getConfDir(String id) {
    return new File(getRootDir(id), "conf");
  }
  
  public File getLibDir(String id) {
    return new File(getRootDir(id), "lib");
  }
  
  public File getLogsDir(String id) {
    return new File(getRootDir(id), "logs");
  }

  public File getScriptFile(String id) {
    return new File(getBinDir(id), "dcc-portal-api");
  }

  public File getMetadataFile(String id) {
    return new File(getRootDir(id), "portal.json");
  }

  public File getJarFile(String id) {
    return new File(getLibDir(id), "dcc-portal-api.jar");
  }
  
  public File getLogFile(String id) {
    return new File(getLibDir(id), "dcc-portal-api.log");
  }
  
  
}
