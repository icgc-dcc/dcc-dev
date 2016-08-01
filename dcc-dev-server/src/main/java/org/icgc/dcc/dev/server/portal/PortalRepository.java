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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Repository for persisting portal instance metadata.
 */
@Repository
public class PortalRepository {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(FAIL_ON_UNKNOWN_PROPERTIES, false) // For schema evolution
      .enable(INDENT_OUTPUT); // For humans

  /**
   * Dependencies.
   */
  @Autowired
  PortalFileSystem fileSystem;

  public List<Integer> getIds() {
    String[] portalIds = fileSystem.getDir().list();
    if (portalIds == null) return emptyList();

    return Stream.of(portalIds).map(Integer::parseInt).collect(toImmutableList());
  }

  public boolean exists(@NonNull Integer portalId) {
    val metadataFile = getMetadataFile(portalId);
    return metadataFile.exists();
  }

  public List<Portal> list() {
    return getIds().stream()
        .map(this::find)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  public Optional<Portal> find(@NonNull Integer portalId) {
    val metadataFile = getMetadataFile(portalId);
    if (!metadataFile.exists()) {
      return Optional.empty();
    }

    return Optional.of(read(metadataFile));
  }

  public void create(@NonNull Portal portal) {
    val metadataFile = getMetadataFile(portal.getId());
    if (metadataFile.exists()) {
      throw new IllegalStateException("Portal " + portal.getId() + " already exists!");
    }

    write(metadataFile, portal);
  }

  public void update(@NonNull Portal portal) {
    val metadataFile = getMetadataFile(portal.getId());
    if (!metadataFile.exists()) {
      throw new IllegalStateException("Portal " + portal.getId() + " no longer exists!");
    }

    write(metadataFile, portal);
  }

  @SneakyThrows
  private Portal read(File file) {
    return MAPPER.readValue(file, Portal.class);
  }

  @SneakyThrows
  private void write(File file, Portal portal) {
    MAPPER.writeValue(file, portal);
  }

  private File getMetadataFile(Integer portalId) {
    return fileSystem.getMetadataFile(portalId);
  }

}
