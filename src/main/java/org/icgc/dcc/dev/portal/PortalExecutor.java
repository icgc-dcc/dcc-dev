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

  public String start(String id) {
    return executeScript(id, "start");
  }

  public String stop(String id) {
    return executeScript(id, "stop");
  }

  public String restart(String id) {
    return executeScript(id, "restart");
  }
  
  public String status(String id) {
    return executeScript(id, "status");
  }

  @SneakyThrows
  private String executeScript(String id, String command) {
    val scriptFile = fileSystem.getScriptFile(id);

    return new ProcessExecutor()
        .command(scriptFile.getAbsolutePath(), command)
        .readOutput(true)
        .execute()
        .outputUTF8();
  }
  
}
