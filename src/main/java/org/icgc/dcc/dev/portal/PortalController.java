package org.icgc.dcc.dev.portal;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.dev.portal.Portal.Candidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class PortalController {

  /**
   * Dependencies.
   */
  @Autowired
  PortalService service;

  @GetMapping("/candidates")
  public List<Candidate> getCandidates() {
    return service.getCandidates();
  }

  @GetMapping("/portals")
  public List<Portal> list() {
    return service.list();
  }

  @PostMapping("/portals")
  public Portal create(
      @RequestParam(value = "pr", required = true) String pr,

      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "ticket", required = false) String ticket,
      @RequestParam(value = "properties", required = false) Map<String, String> properties) {
    return service.create(pr, name, title, description, ticket, properties);
  }

  @PutMapping("/portals/{id}")
  public Portal update(
      @PathVariable("id") String id,

      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "ticket", required = false) String ticket,
      @RequestParam(value = "properties", required = false) Map<String, String> properties) {
    return service.update(id, name, title, description, ticket, properties);
  }

  @GetMapping("/portals/{id}")
  public Portal get(@PathVariable("id") String id) {
    return service.get(id);
  }

  @GetMapping("/portals/{id}/status")
  public String status(@PathVariable("id") String id) {
    return service.status(id);
  }

  @DeleteMapping("/portals/{id}")
  public void remove(@PathVariable("id") String id) {
    service.remove(id);
  }

  @PostMapping("/portals/{id}/start")
  public void start(@PathVariable("id") String id) {
    service.start(id);
  }

  @PostMapping("/portals/{id}/stop")
  public void stop(@PathVariable("id") String id) {
    service.stop(id);
  }

  @PostMapping("/portals/{id}/restart")
  public void restart(@PathVariable("id") String id) {
    service.restart(id);
  }

}
