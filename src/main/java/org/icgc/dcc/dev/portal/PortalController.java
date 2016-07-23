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

  @PutMapping("/portals/{portalId}")
  public Portal update(
      @PathVariable("portalId") String portalId,

      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "ticket", required = false) String ticket,
      @RequestParam(value = "properties", required = false) Map<String, String> properties) {
    return service.update(portalId, name, title, description, ticket, properties);
  }

  @GetMapping("/portals/{portalId}")
  public Portal get(@PathVariable("portalId") String portalId) {
    return service.get(portalId);
  }

  @GetMapping("/portals/{portalId}/status")
  public String status(@PathVariable("portalId") String portalId) {
    return service.status(portalId);
  }

  @DeleteMapping("/portals/{portalId}")
  public void remove(@PathVariable("portalId") String portalId) {
    service.remove(portalId);
  }

  @PostMapping("/portals/{portalId}/start")
  public void start(@PathVariable("portalId") String portalId) {
    service.start(portalId);
  }

  @PostMapping("/portals/{portalId}/stop")
  public void stop(@PathVariable("portalId") String portalId) {
    service.stop(portalId);
  }

  @PostMapping("/portals/{portalId}/restart")
  public void restart(@PathVariable("portalId") String portalId) {
    service.restart(portalId);
  }

}
