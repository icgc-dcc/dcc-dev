package org.icgc.dcc.dev.log;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableList;

@Component
@RestController
public class LogController {

  @RequestMapping("/logs")
  public List<String> portals() {
    return ImmutableList.of();
  }
  
}
