package org.icgc.dcc.dev.portal;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import com.google.api.client.util.Lists;

import lombok.SneakyThrows;
import lombok.val;

@Component
public class PortalExecutor {

  @Autowired
  PortalFileSystem fileSystem;

  public String start(String portalId, Map<String, String> arguments) {
    return executeScript(portalId, "start", arguments);
  }

  public String restart(String portalId, Map<String, String> arguments) {
    return executeScript(portalId, "restart", arguments);
  }

  public String stop(String portalId) {
    return executeScript(portalId, "stop", null);
  }

  public String status(String portalId) {
    return executeScript(portalId, "status", null);
  }

  @SneakyThrows
  private String executeScript(String portalId, String action, Map<String, String> arguments) {
    val scriptFile = fileSystem.getScriptFile(portalId);

    return new ProcessExecutor()
        .command(createCommand(scriptFile, action, arguments))
        .readOutput(true)
        .execute()
        .outputUTF8();
  }

  private static List<String> createCommand(File scriptFile, String action, Map<String, String> arguments) {
    val command = Lists.<String> newArrayList();
    command.add(scriptFile.getAbsolutePath());
    command.add(action);
    command.addAll(createCommandArgs(arguments));

    return command;
  }

  private static List<String> createCommandArgs(Map<String, String> arguments) {
    if (arguments == null) {
      return emptyList();
    }

    return arguments.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue()).collect(toList());
  }

}
