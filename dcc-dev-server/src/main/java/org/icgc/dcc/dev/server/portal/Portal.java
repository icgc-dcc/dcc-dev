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

import static com.google.common.collect.Maps.newHashMap;
import static javax.persistence.FetchType.EAGER;

import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Version;

import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.jira.JiraTicket;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Collection of metadata that represents a portal instance.
 * <p>
 * Main user facing system entity.
 */
@Entity
@Data
@Accessors(chain = true)
public class Portal {

  /**
   * The unique identifier for the portal instance.
   * <p>
   * Primary key.
   */
  @Id
  @GeneratedValue
  Integer id;

  /**
   * A symbolic addressable URL slug for the portal instance.
   */
  String slug;

  /**
   * A short title for the portal instance.
   */
  String title;

  /**
   * A longer description for the portal instance.
   */
  @Lob
  String description;

  /**
   * The JIRA ticket (issue) key for the portal instance.
   */
  String ticketKey;

  /**
   * User supplied configuration.
   */
  @ElementCollection(fetch = EAGER)
  @CollectionTable
  Map<String, String> config = newHashMap();

  /**
   * System supplied configuration.
   */
  @ElementCollection(fetch = EAGER)
  @CollectionTable
  Map<String, String> systemConfig = newHashMap();

  /**
   * The absolute URL of the running portal instance.
   */
  String url;

  /**
   * Whether or not to automatically deploy a new build when available.
   */
  boolean autoDeploy;

  /**
   * Whether or not to automatically destroy when a PR is merged.
   */
  boolean autoRemove;

  /**
   * The upstream candidate information about the running portal instance.
   */
  @Embedded
  Candidate target;

  /**
   * Optimistic locking version.
   */
  @Version
  int version = 0;

  /**
   * A candidate for portal instance deployment.
   */
  @Data
  @Embeddable
  @Accessors(chain = true)
  public static class Candidate {

    @Embedded
    GithubPr pr;
    @Embedded
    JenkinsBuild build;
    String artifact;
    @Embedded
    JiraTicket ticket;

  }

  /**
   * Runtime status of the executing portal instance.
   */
  @Data
  @Embeddable
  @Accessors(chain = true)
  public static class Status {

    boolean running;

    Integer pid;
    String wrapper;
    String java;

  }

}
