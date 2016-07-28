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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.SneakyThrows;

@Repository
public class PortalRepository {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);

  /**
   * Dependencies.
   */
  @Autowired
  PortalFileSystem fileSystem;

  public List<String> getIds() {
    String[] portalIds = fileSystem.getDir().list();
    if (portalIds == null) return emptyList();
  
    return ImmutableList.copyOf(portalIds);
  }

  public List<Portal> list() {
    return getIds().stream().map(portalId -> get(portalId)).collect(toList());
  }

  public Portal get(@NonNull String portalId) {
    return read(getMetadataFile(portalId));
  }

  public void save(@NonNull Portal portal) {
    write(getMetadataFile(portal.getId()), portal);
  }

  @SneakyThrows
  private Portal read(File file) {
    return MAPPER.readValue(file, Portal.class);
  }

  @SneakyThrows
  private void write(File file, Portal portal) {
    MAPPER.writeValue(file, portal);
  }

  private File getMetadataFile(String portalId) {
    return fileSystem.getMetadataFile(portalId);
  }

}
