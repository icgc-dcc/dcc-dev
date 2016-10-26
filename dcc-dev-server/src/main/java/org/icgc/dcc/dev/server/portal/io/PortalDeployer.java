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

import static java.nio.file.Files.copy;
import static java.nio.file.Files.setPosixFilePermissions;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.icgc.dcc.dev.server.portal.util.Portals.MANAGEMENT_PORT_PROPERTY;
import static org.icgc.dcc.dev.server.portal.util.Portals.SERVER_PORT_PROPERTY;
import static org.springframework.util.SocketUtils.findAvailableTcpPort;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.icgc.dcc.dev.server.portal.Portal;
import org.icgc.dcc.dev.server.portal.util.PortalArchive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for creating the physical environment for running portal instances.
 */
@Slf4j
@Component
public class PortalDeployer {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${template.url}")
  URL templateUrl;
  @Value("${template.dir}")
  File templateDir;
  @Value("${artifact.artifactId}")
  String artifactId;

  /**
   * Dependencies.
   */
  @Autowired
  PortalFileSystem fileSystem;

  @SneakyThrows
  public void setup() {
    log.info("Creating template...");
    templateDir.mkdirs();

    val archive = new PortalArchive(templateUrl, artifactId);
    archive.extract(templateDir);
  }

  @SneakyThrows
  public void init(@NonNull Portal portal) {
    val targetDir = fileSystem.getRootDir(portal.getId());
    if (!targetDir.exists()) {
      copyTemplate(portal.getId(), targetDir);

      // Ensure log dir is created (JSW won't make it but logback will)
      fileSystem.getLogsDir(portal.getId()).mkdir();
    }
  }

  @SneakyThrows
  public void deploy(@NonNull Portal portal) {
    downloadJar(portal);
    assignPorts(portal);
  }

  @SneakyThrows
  public void undeploy(@NonNull Integer portalId) {
    val targetDir = fileSystem.getRootDir(portalId);

    deleteDirectory(targetDir);
  }

  private void copyTemplate(Integer portalId, File targetDir) throws IOException {
    copyDirectory(templateDir, targetDir);

    // Make executable
    File[] binaries = fileSystem.getBinDir(portalId).listFiles();
    for (File binary : binaries) {
      setPosixFilePermissions(binary.toPath(), ImmutableSet.of(OWNER_EXECUTE, OWNER_READ));
    }
  }

  private void downloadJar(Portal portal) throws MalformedURLException, IOException {
    val artifact = portal.getTarget().getArtifact();
    if (artifact == null) {
      log.info("Artifact for portal {} not available (null). Skipping jar download...", portal.getId());
      return;
    }

    val artifactUrl = new URL(artifact);
    val jarFile = fileSystem.getJarFile(portal.getId());

    log.info("Downloading {} to {}", artifactUrl, jarFile);
    copy(artifactUrl.openStream(), jarFile.toPath(), REPLACE_EXISTING);
  }

  private static void assignPorts(Portal portal) {
    val systemConfig = portal.getSystemConfig();
    assignPort(systemConfig, SERVER_PORT_PROPERTY);
    assignPort(systemConfig, MANAGEMENT_PORT_PROPERTY);
    log.info("Ports: {}", systemConfig);
  }

  private static void assignPort(Map<String, String> systemConfig, String portProperty) {
    // Give preference to the current value, if any
    val portStart = Integer.valueOf(systemConfig.getOrDefault(portProperty, "8000"));
    val portEnd = 9000;
    val port = findAvailableTcpPort(portStart, portEnd);

    systemConfig.put(portProperty, String.valueOf(port));
  }

}
