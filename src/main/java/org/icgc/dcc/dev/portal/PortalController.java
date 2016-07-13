package org.icgc.dcc.dev.portal;

import java.util.List;

import org.icgc.dcc.dev.deploy.DeployService;
import org.icgc.dcc.dev.metadata.Metadata;
import org.icgc.dcc.dev.metadata.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;

@Component
@RestController
public class PortalController {
  
  @Autowired
  MetadataService metadata;
  @Autowired
  DeployService deploy;

  @RequestMapping("/portals")
  public List<Metadata> portals() {
    return ImmutableList.of();
  }
  
  @RequestMapping("/echo")
  public String deploy() {
    return deploy.echo();
  }
  
}
