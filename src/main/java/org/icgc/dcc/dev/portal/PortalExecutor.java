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
    return execute(id, "start");
  }

  public String stop(String id) {
    return execute(id, "stop");
  }

  public String restart(String id) {
    return execute(id, "restart");
  }

  @SneakyThrows
  private String execute(String id, String command) {
    val executable = fileSystem.getExecutableFile(id);

    return new ProcessExecutor()
        .command(executable.getAbsolutePath(), command)
        .readOutput(true)
        .execute()
        .outputUTF8();
  }
  
}
