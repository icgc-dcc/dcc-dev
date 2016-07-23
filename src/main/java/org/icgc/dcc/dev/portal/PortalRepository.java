package org.icgc.dcc.dev.portal;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

@Service
public class PortalRepository {

  private static final ObjectMapper MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);

  @Autowired
  PortalFileSystem fileSystem;

  public List<Portal> list() {
    return resolvePortalIds().stream().map(this::get).collect(toList());
  }

  public Portal get(String portalId) {
    return read(file(portalId));
  }

  public void save(Portal portal) {
    write(file(portal.getId()), portal);
  }

  @SneakyThrows
  private Portal read(File file) {
    return MAPPER.readValue(file, Portal.class);
  }

  @SneakyThrows
  private void write(File file, Portal portal) {
    MAPPER.writeValue(file, portal);
  }

  private File file(String portalId) {
    return fileSystem.getMetadataFile(portalId);
  }

  private List<String> resolvePortalIds() {
    return copyOf(fileSystem.getDir().list());
  }

}
