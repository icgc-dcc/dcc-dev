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
package org.icgc.dcc.dev.server.artifactory;

import java.util.List;
import java.util.Optional;

import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.Searches;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.jfrog.artifactory.client.model.RepoPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Service
public class ArtifactoryService {

  /**
   * Constants.
   */
  private static final String BUILD_NUMBER_PROPERTY_NAME = "build.number";

  /**
   * Configuration.
   */
  @Value("${artifact.repoName}")
  String repoName;
  @Value("${artifact.groupId}")
  String groupId;
  @Value("${artifact.artifactId}")
  String artifactId;

  /**
   * Dependencies.
   */
  @Autowired
  Artifactory artifactory;

  @SneakyThrows
  public List<RepoPath> list() {
    return prepareSearch().doSearch();
  }

  @SneakyThrows
  public Optional<String> getArtifact(@NonNull String buildNumber) {
    val paths = prepareSearch().itemsByProperty().property(BUILD_NUMBER_PROPERTY_NAME, buildNumber).doSearch();
    return paths.stream()
        .filter(this::isPrimaryArifact)
        .findFirst()
        .map(RepoPath::getItemPath)
        .map(this::resolveAbsolutePath);
  }

  @SneakyThrows
  public List<Item> getArtifactFolder() {
    val path = resolveGroupPath(groupId) + "/" + artifactId;
    Folder folder = artifactory.repository(repoName).folder(path).info();

    return folder.getChildren();
  }

  private Searches prepareSearch() {
    return artifactory.searches().repositories(repoName).artifactsByName(artifactId);
  }

  private boolean isPrimaryArifact(RepoPath p){
    return  p.getItemPath().contains(artifactId) && p.getItemPath().endsWith(".jar");
  }

  private String resolveAbsolutePath(final java.lang.String path) {
    return artifactory.getUri() + "/" + artifactory.getContextName() + "/" + repoName + "/" + path;
  }

  private static String resolveGroupPath(String groupId) {
    return groupId.replaceAll("\\.", "/");
  }

}
