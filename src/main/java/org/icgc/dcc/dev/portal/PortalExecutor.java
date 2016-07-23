package org.icgc.dcc.dev.portal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import lombok.SneakyThrows;
import lombok.val;

@Component
public class PortalExecutor {

  @Autowired
  PortalFileSystem fileSystem;

  public String start(String portalId) {
    return executeScript(portalId, "start");
  }

  public String stop(String portalId) {
    return executeScript(portalId, "stop");
  }

  public String restart(String portalId) {
    return executeScript(portalId, "restart");
  }
  
  public String status(String portalId) {
    return executeScript(portalId, "status");
  }

  @SneakyThrows
  private String executeScript(String portalId, String command) {
    val scriptFile = fileSystem.getScriptFile(portalId);

    return new ProcessExecutor()
        .command(scriptFile.getAbsolutePath(), command)
        .readOutput(true)
        .execute()
        .outputUTF8();
  }
  
}
