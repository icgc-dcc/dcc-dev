package org.icgc.dcc.dev.metadata;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;

@Component
@RestController
public class MetadataController {

  @RequestMapping("/portals")
  public List<Metadata> portals() {
    return ImmutableList.of();
  }
  
}
