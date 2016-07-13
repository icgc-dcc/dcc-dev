package org.icgc.dcc.dev.deploy;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zeroturnaround.exec.ProcessExecutor;

import lombok.SneakyThrows;
import lombok.val;

@Service
public class DeployService {
  
  @Value("${workspace}")
  File workspace;

  @SneakyThrows
  public String deploy(String id, String buildNumber) {
    val installer = getInstaller(id);

    return new ProcessExecutor()
        .command(installer.getAbsolutePath(), "-p", buildNumber)
        .readOutput(true)
        .execute()
        .outputUTF8();
  }
  
  @SneakyThrows
  public String echo() {
    return new ProcessExecutor()
        .command("echo", "Hi!")
        .readOutput(true)
        .execute()
        .outputUTF8();
  }

  private File getInstaller(String id) {
    val bin = new File(workspace, "bin");
    
    return new File(bin, "install");
  }

}
