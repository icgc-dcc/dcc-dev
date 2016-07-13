package org.icgc.dcc.dev.github;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class GithubController {

  @Autowired
  GithubService github;
  
  @RequestMapping("/prs")
  public List<String> prs() {
    return github.getPrs();
  }

}
