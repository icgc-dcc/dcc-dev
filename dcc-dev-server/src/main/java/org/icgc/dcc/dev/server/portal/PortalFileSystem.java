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

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;

@Component
public class PortalFileSystem {

  /**
   * Configuration.
   */
  @Value("${workspace.dir}")
  File workspaceDir;
  @Value("${artifact.artifactId}")
  String baseName = "dcc-portal-server";

  public File getDir() {
    return new File(workspaceDir, "portals");
  }

  public File getRootDir(@NonNull String portalId) {
    return new File(getDir(), portalId);
  }

  public File getBinDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "bin");
  }

  public File getSettingsFile(@NonNull String portalId) {
    return new File(getConfDir(portalId), "application.yml");
  }

  public File getConfDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "conf");
  }

  public File getLibDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "lib");
  }

  public File getLogsDir(@NonNull String portalId) {
    return new File(getRootDir(portalId), "logs");
  }

  public File getScriptFile(@NonNull String portalId) {
    return new File(getBinDir(portalId), baseName);
  }

  public File getMetadataFile(@NonNull String portalId) {
    return new File(getRootDir(portalId), "portal.json");
  }

  public File getJarFile(@NonNull String portalId) {
    return new File(getLibDir(portalId), baseName + ".jar");
  }

  public File getLogFile(@NonNull String portalId) {
    return new File(getLogsDir(portalId), baseName + ".log");
  }

}
