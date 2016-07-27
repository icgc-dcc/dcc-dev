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
package org.icgc.dcc.dev.portal;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;

import org.icgc.dcc.dev.github.GithubPr;
import org.icgc.dcc.dev.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.jira.JiraTicket;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Portal {

  /**
   * The unique identifier for the portal instance.
   * <p>
   * Primary key.
   */
  String id;

  /**
   * A symbolic name for the portal instance.
   */
  String name;

  /**
   * A short title for the portal instance.
   */
  String title;

  /**
   * A longer description for the portal instance.
   */
  String description;

  /**
   * The JIRA ticket number for the portal instance.
   */
  String ticket;

  /**
   * User supplied configuration properties.
   */
  Map<String, String> properties = newHashMap();

  /**
   * System supplied configuration properties.
   */
  Map<String, String> systemProperties = newHashMap();

  /**
   * The absolute URL of the running portal instance.
   */
  String url;

  /**
   * The execution state of the portal instance.
   */
  State state = State.NEW;

  /**
   * Whether or not to automatically deploy a new build when available.
   */
  boolean autoDeploy;
  
  /**
   * The upstream canidate information about the running portal instance.
   */
  Candidate target;

  @Data
  @Accessors(chain = true)
  public static class Candidate {

    GithubPr pr;
    JenkinsBuild build;
    String artifact;
    JiraTicket ticket;

  }

  public static enum State {

    NEW, STARTING, RUNNING, STOPPING, STOPPED, RESTARTING, FAILED;

  }

}
