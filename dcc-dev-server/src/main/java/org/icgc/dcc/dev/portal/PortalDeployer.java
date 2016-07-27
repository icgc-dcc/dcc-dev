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

import static com.google.common.collect.Ordering.natural;
import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PortalDeployer {

  /**
   * Configuration.
   */
  @Value("${template.url}")
  URL templateUrl;
  @Value("${template.dir}")
  File templateDir;

  /**
   * Dependencies.
   */
  @Autowired
  PortalFileSystem fileSystem;

  @SneakyThrows
  public void setup() {
    log.info("Creating template...");
    templateDir.mkdirs();

    val archive = new PortalArchive(templateUrl);
    archive.extract(templateDir);
  }

  @SneakyThrows
  public void deploy(@NonNull Portal portal) {
    val portalId = nextPortalId();
    portal.setId(portalId);

    val targetDir = fileSystem.getRootDir(portalId);
    if (!targetDir.exists()) {
      copyTemplate(portalId, targetDir);
    }

    downloadJar(portal);
  }

  @SneakyThrows
  public void update(@NonNull Portal portal) {
    downloadJar(portal);
  }

  @SneakyThrows
  public void undeploy(@NonNull String portalId) {
    val targetDir = fileSystem.getRootDir(portalId);

    deleteDirectory(targetDir);
  }

  private void copyTemplate(String portalId, File targetDir) throws IOException {
    copyDirectory(templateDir, targetDir);
    
    // Make executable
    val binaries = fileSystem.getBinDir(portalId).listFiles();
    for (val binary : binaries) {
      Files.setPosixFilePermissions(binary.toPath(), ImmutableSet.of(OWNER_EXECUTE, OWNER_READ));
    }
  }

  private void downloadJar(Portal portal) throws MalformedURLException, IOException {
    val artifactUrl = new URL(portal.getTarget().getArtifact());
    val jarFile = fileSystem.getJarFile(portal.getId());

    log.info("Downloading {} to {}", artifactUrl, jarFile);
    copy(artifactUrl.openStream(), jarFile.toPath(), REPLACE_EXISTING);
  }

  private String nextPortalId() {
    val portalId = resolvePortalIds().stream().map(Integer::valueOf).sorted(natural().reversed()).findFirst().orElse(0);

    // Advance
    return String.valueOf(portalId + 1);
  }

  private List<String> resolvePortalIds() {
    String[] portalIds = fileSystem.getDir().list();
    if (portalIds == null) return emptyList();

    return ImmutableList.copyOf(portalIds);
  }

}
