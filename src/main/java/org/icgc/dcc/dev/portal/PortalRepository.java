package org.icgc.dcc.dev.portal;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.SneakyThrows;

@Service
public class PortalRepository {

  private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  @Autowired
  PortalFileSystem fileSystem;

  public List<Portal> list() {
    return resolveIds().stream().map(this::get).collect(toList());
  }

  public Portal get(String id) {
    return read(file(id));
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

  private File file(String id) {
    return fileSystem.getMetadataFile(id);
  }

  private List<String> resolveIds() {
    return copyOf(fileSystem.getDir().list());
  }

}
