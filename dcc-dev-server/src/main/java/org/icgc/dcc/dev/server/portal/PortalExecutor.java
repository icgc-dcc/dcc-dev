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
package org.icgc.dcc.dev.server.portal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.dev.server.portal.Portal.State.RESTARTING;
import static org.icgc.dcc.dev.server.portal.Portal.State.RUNNING;
import static org.icgc.dcc.dev.server.portal.Portal.State.STARTING;
import static org.icgc.dcc.dev.server.portal.Portal.State.STOPPED;
import static org.icgc.dcc.dev.server.portal.Portal.State.STOPPING;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.icgc.dcc.dev.server.message.MessageService;
import org.icgc.dcc.dev.server.message.Messages.ExecutionMessage;
import org.icgc.dcc.dev.server.message.Messages.StateMessage;
import org.icgc.dcc.dev.server.portal.Portal.State;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PortalExecutor {

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
  PortalRepository repository;
  @Autowired
  MessageService messages;

  public void start(@NonNull Portal portal) {
    updateState(portal, STARTING);
    executeScript(portal.getId(), "start", resolveArguments(portal));
    updateState(portal, RUNNING);
  }

  public void restart(@NonNull Portal portal) {
    updateState(portal, RESTARTING);
    executeScript(portal.getId(), "restart", resolveArguments(portal));
    updateState(portal, RUNNING);
  }

  public void stop(@NonNull Portal portal) {
    updateState(portal, STOPPING);
    executeScript(portal.getId(), "stop", null);
    updateState(portal, STOPPED);
  }

  public String status(@NonNull String portalId) {
    return executeScript(portalId, "status", null);
  }
  
  private void updateState(Portal portal, State state) {
    repository.update(portal.setState(state));
    messages.sendMessage(new StateMessage().setState(state).setPortalId(portal.getId()));
  }

  @SneakyThrows
  private String executeScript(String portalId, String action, Map<String, String> arguments) {
    val scriptFile = fileSystem.getScriptFile(portalId);
    val command = createCommand(scriptFile, action, arguments);

    log.info("Executing command: {}", command);
    val output = new ProcessExecutor()
        .command(command)
        .readOutput(true)
        .execute()
        .outputUTF8();

    log.info("Output: {}", output);
    messages.sendMessage(new ExecutionMessage().setAction(action).setOutput(output).setPortalId(portalId));

    return output;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Map<String, String> resolveArguments(Portal portal) {
    val effectiveArguments = Maps.<String, String> newHashMap();
    effectiveArguments.putAll(portal.getProperties() == null ? emptyMap() : portal.getProperties());
    effectiveArguments.putAll((Map) config);
    effectiveArguments.putAll(portal.getSystemProperties());

    return effectiveArguments;
  }

  private static List<String> createCommand(File scriptFile, String action, Map<String, String> arguments) {
    return ImmutableList.<String> builder()
        .add(scriptFile.getAbsolutePath())
        .add(action)
        .addAll(createCommandArgs(arguments)).build();
  }

  private static List<String> createCommandArgs(Map<String, String> arguments) {
    if (arguments == null) return emptyList();

    return arguments.entrySet().stream()
        .map(e -> "--" + e.getKey() + "=" + e.getValue())
        .collect(toList());
  }

}
