/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.dev.server.portal.io;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.dev.server.message.Messages.PortalChangeMessage.portalChange;
import static org.icgc.dcc.dev.server.portal.Portal.State.RESTARTING;
import static org.icgc.dcc.dev.server.portal.Portal.State.RUNNING;
import static org.icgc.dcc.dev.server.portal.Portal.State.STARTING;
import static org.icgc.dcc.dev.server.portal.Portal.State.STOPPED;
import static org.icgc.dcc.dev.server.portal.Portal.State.STOPPING;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.PortalChangeMessage.Type;
import org.icgc.dcc.dev.server.portal.Portal;
import org.icgc.dcc.dev.server.portal.Portal.State;
import org.icgc.dcc.dev.server.portal.Portal.Status;
import org.icgc.dcc.dev.server.portal.util.PortalLocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for executing commands against a portal instance.
 */
@Slf4j
@Component
public class PortalExecutor {

  /**
   * Constants.
   */
  private static final String STATUS_RUNNING_VALUE = "running";
  private static final Pattern STATUS_PATTERN =
      Pattern.compile("DCC Portal Server is ([^:.]+)[:.](?: PID:(\\d+), Wrapper:(\\w+), Java:(\\w+))?\n");

  /**
   * Configuration.
   */
  @Autowired
  @Qualifier("config")
  Properties config;

  /**
   * Dependencies.
   */
  @Autowired
  PortalFileSystem fileSystem;
  @Autowired
  PortalLocks locks;
  @Autowired
  MessageService messages;

  public Status getStatus(@NonNull Integer portalId) {
    val statusOutput = executeScript(portalId, CommandName.STATUS, null);
    return parseStatus(statusOutput);
  }

  public void start(@NonNull Portal portal) {
    update(portal, STARTING);
    executeScript(portal.getId(), CommandName.START, resolveArguments(portal));
    update(portal, RUNNING);
  }

  @Async
  public void startAsync(@NonNull Portal portal) {
    start(portal);
  }

  public void restart(@NonNull Portal portal) {
    update(portal, RESTARTING);
    executeScript(portal.getId(), CommandName.RESTART, resolveArguments(portal));
    update(portal, RUNNING);
  }

  @Async
  public void restartAsync(@NonNull Portal portal) {
    restart(portal);
  }

  public void stop(@NonNull Portal portal) {
    update(portal, STOPPING);
    executeScript(portal.getId(), CommandName.STOP, null);
    update(portal, STOPPED);
  }

  @Async
  public void stopAsync(@NonNull Portal portal) {
    stop(portal);
  }

  private void update(Portal portal, State state) {
    // Notify
    messages.sendMessage(portalChange().type(Type.UPDATED).portal(portal));
  }

  @SneakyThrows
  private String executeScript(Integer portalId, CommandName commandName, Map<String, String> arguments) {
    @Cleanup
    val lock = locks.lockReading(portalId);

    val scriptFile = fileSystem.getScriptFile(portalId);
    val command = createCommand(scriptFile, commandName, arguments);

    log.info("Executing command: {}", command);
    val output = new ProcessExecutor()
        .command(command)
        .readOutput(true)
        .execute()
        .outputUTF8();

    log.info("Output: {}", output);
    return output;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Map<String, String> resolveArguments(Portal portal) {
    val effectiveArguments = Maps.<String, String> newHashMap();
    effectiveArguments.putAll(portal.getConfig() == null ? emptyMap() : portal.getConfig());
    effectiveArguments.putAll((Map) config);
    effectiveArguments.putAll(portal.getSystemConfig());

    return effectiveArguments;
  }

  private static List<String> createCommand(File scriptFile, CommandName commandName, Map<String, String> arguments) {
    return ImmutableList.<String> builder()
        .add(scriptFile.getAbsolutePath())
        .add(commandName.getId())
        .addAll(createCommandArgs(arguments)).build();
  }

  private static List<String> createCommandArgs(Map<String, String> arguments) {
    if (arguments == null) return emptyList();

    return arguments.entrySet().stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue()) // Format Spring Boot argument
        .collect(toList());
  }

  private static Status parseStatus(String statusOutput) {
    val matcher = STATUS_PATTERN.matcher(statusOutput);
    checkState(matcher.matches(), "Expected '%s' to match pattern: %s", statusOutput, STATUS_PATTERN);

    // Parse
    int i = 1;
    val state = matcher.group(i++);
    val pid = matcher.group(i++);
    val wrapper = matcher.group(i++);
    val java = matcher.group(i++);

    return new Status()
        .setRunning(state.equals(STATUS_RUNNING_VALUE))
        .setPid(pid == null ? null : Integer.valueOf(pid))
        .setWrapper(wrapper)
        .setJava(java);
  }

  /**
   * Command type.
   */
  @RequiredArgsConstructor
  private enum CommandName {

    START("start"), RESTART("restart"), STOP("stop"), STATUS("status");

    @Getter
    private final String id;

  }

}
