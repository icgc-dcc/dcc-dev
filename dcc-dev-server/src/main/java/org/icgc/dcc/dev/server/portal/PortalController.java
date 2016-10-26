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

import static org.icgc.dcc.dev.server.portal.util.Portals.getWebBaseUrl;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.ResponseEntity.ok;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.portal.Portal.Candidate;
import org.icgc.dcc.dev.server.portal.Portal.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * HTTP bindings for interaction with portal instances.
 */
@RestController
@CrossOrigin(exposedHeaders = "X-Server-Timestamp")
@RequiredArgsConstructor
public class PortalController {

  /**
   * Dependencies.
   */
  final PortalService service;

  /**
   * Redirects by {@code slug} to the associated portal's {@code url}. <br>
   * Useful as a mnemonic URL for users.
   */
  @GetMapping("/portals/{slug:\\D.*}")
  public void redirect(@PathVariable("slug") String slug, HttpServletResponse response) throws IOException {
    val portal = service.getBySlug(slug);
    response.sendRedirect(getWebBaseUrl(portal));
  }

  /**
   * Redirects by {@code id} to the associated portal's {@code url}. <br>
   * Useful as a mnemonic URL for users.
   */
  @GetMapping("/portals/{portalId:\\d+}")
  public void redirect(@PathVariable("portalId") Integer portalId, HttpServletResponse response) throws IOException {
    val portal = service.get(portalId);
    response.sendRedirect(getWebBaseUrl(portal));
  }

  /**
   * Gets a PR by id.
   */
  @GetMapping("/api/prs/{prNumber}")
  public GithubPr getPr(@PathVariable("prNumber") Integer prNumber) {
    return service.getPr(prNumber);
  }

  /**
   * Lists the candidates for deployment.
   */
  @GetMapping("/api/candidates")
  public List<Candidate> getCandidates() {
    return service.getCandidates();
  }

  /**
   * Lists the currently deployed portals.
   */
  @GetMapping("/api/portals")
  public List<Portal> list() {
    return service.list();
  }

  /**
   * Gets the portal with the supplied {@code portalId}.
   */
  @GetMapping("/api/portals/{portalId}")
  public Portal get(@PathVariable("portalId") Integer portalId) {
    return service.get(portalId);
  }

  /**
   * Gets the full content of portal log with the supplied {@code portalId}.
   */
  @GetMapping("/api/portals/{portalId}/log")
  public ResponseEntity<String> getLog(@PathVariable("portalId") Integer portalId) {
    val portalLog = service.getLog(portalId);
    val serverTimestamp = String.valueOf(System.currentTimeMillis());

    return ok().header("X-Server-Timestamp", serverTimestamp).body(portalLog);
  }

  /**
   * Lists all of the currently deployed portals.
   */
  @PostMapping("/api/portals")
  @ResponseStatus(ACCEPTED)
  public Portal create(
      @RequestParam(value = "prNumber", required = true) Integer prNumber,

      @RequestParam(value = "slug", required = false) String slug,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "ticket", required = false) String ticket,
      @RequestParam(value = "config", required = false) Map<String, String> config,

      @RequestParam(value = "autoDeploy", required = false, defaultValue = "true") boolean autoDeploy,
      @RequestParam(value = "autoRefresh", required = false, defaultValue = "true") boolean autoRefresh,
      @RequestParam(value = "autoRemove", required = false, defaultValue = "true") boolean autoRemove,

      @RequestParam(value = "start", required = false, defaultValue = "true") boolean start,

      HttpServletRequest request) {
    val username = getUsername(request);

    return service.create(prNumber, slug, title, description, ticket, config, autoDeploy, autoRefresh, autoRemove,
        username, start);
  }

  /**
   * Updates the portal with the specified {@code portalId}.
   */
  @PutMapping("/api/portals/{portalId}")
  @ResponseStatus(ACCEPTED)
  public Portal update(
      @PathVariable("portalId") Integer portalId,

      @RequestParam(value = "slug", required = false) String slug,
      @RequestParam(value = "title", required = false) String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "ticket", required = false) String ticket,
      @RequestParam(value = "config", required = false) Map<String, String> config,

      @RequestParam(value = "autoDeploy", required = false, defaultValue = "true") boolean autoDeploy,
      @RequestParam(value = "autoRefresh", required = false, defaultValue = "true") boolean autoRefresh,
      @RequestParam(value = "autoRemove", required = false, defaultValue = "true") boolean autoRemove) {
    return service.update(portalId, slug, title, description, ticket, config, autoDeploy, autoRefresh, autoRemove);
  }

  /**
   * Gets the portal execution status with the specified {@code portalId}.
   */
  @GetMapping("/api/portals/{portalId}/status")
  public Status status(@PathVariable("portalId") Integer portalId) {
    return service.getStatus(portalId);
  }

  /**
   * Starts the portal the specified {@code portalId}.
   */
  @PostMapping("/api/portals/{portalId}/start")
  @ResponseStatus(ACCEPTED)
  public void start(@PathVariable("portalId") Integer portalId) {
    service.start(portalId);
  }

  /**
   * Stops the portal the specified {@code portalId}.
   */
  @PostMapping("/api/portals/{portalId}/stop")
  @ResponseStatus(ACCEPTED)
  public void stop(@PathVariable("portalId") Integer portalId) {
    service.stop(portalId);
  }

  /**
   * Restarts the portal the specified {@code portalId}.
   */
  @PostMapping("/api/portals/{portalId}/restart")
  @ResponseStatus(ACCEPTED)
  public void restart(@PathVariable("portalId") Integer portalId) {
    service.restart(portalId);
  }

  /**
   * Removes the portal the specified {@code portalId}.
   */
  @DeleteMapping("/api/portals/{portalId}")
  @ResponseStatus(ACCEPTED)
  public void remove(@PathVariable("portalId") Integer portalId) {
    service.remove(portalId);
  }

  /**
   * Removes all deployed portals.
   */
  @DeleteMapping("/api/portals")
  @ResponseStatus(ACCEPTED)
  public void remove() {
    service.remove();
  }

  private static String getUsername(HttpServletRequest request) {
    return request.getRemoteHost();
  }

}
