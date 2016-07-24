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
package org.icgc.dcc.dev.portal;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.ProcessExecutor;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Component
public class PortalExecutor {

  /**
   * Dependencies.
   */
  @Autowired
  PortalFileSystem fileSystem;

  public String start(@NonNull String portalId, Map<String, String> arguments) {
    return executeScript(portalId, "start", arguments);
  }

  public String restart(@NonNull String portalId, Map<String, String> arguments) {
    return executeScript(portalId, "restart", arguments);
  }

  public String stop(@NonNull String portalId) {
    return executeScript(portalId, "stop", null);
  }

  public String status(@NonNull String portalId) {
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
    return ImmutableList.<String> builder()
        .add(scriptFile.getAbsolutePath())
        .add(action)
        .addAll(createCommandArgs(arguments)).build();
  }

  private static List<String> createCommandArgs(Map<String, String> arguments) {
    if (arguments == null) return emptyList();

    return arguments.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue()).collect(toList());
  }

}
