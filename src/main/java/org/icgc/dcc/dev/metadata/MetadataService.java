package org.icgc.dcc.dev.metadata;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

@Service
public class MetadataService {

  @Autowired
  ObjectMapper mapper;
  @Value("${metadata.dir}")
  File dir;

  @SneakyThrows
  public List<Metadata> list() {
    return Files.list(dir.toPath()).map(this::read).collect(toList());
  }

  public Metadata get(String id) {
    return read(path(id));
  }

  public void save(Metadata metadata) {
    write(path(metadata.getId()), metadata);
  }
  
  @SneakyThrows
  private Metadata read(Path path) {
    return mapper.readValue(path.toFile(), Metadata.class);
  }
  
  @SneakyThrows
  private void write(Path path, Metadata metadata) {
    mapper.writeValue(path.toFile(), metadata);
  }

  private Path path(String id) {
    return new File(dir, id + ".json").toPath();
  }

}
