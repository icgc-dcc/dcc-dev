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
package org.icgc.dcc.dev.server.message;

import java.util.List;

import org.icgc.dcc.dev.server.github.GithubPr;
import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.portal.io.PortalExecutor.State;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Catalog of messages to be sent from publishers to subscribers.
 */
@NoArgsConstructor
public final class Messages {

  @Value
  @Builder
  public static class JenkinsBuildsMessage {

    List<JenkinsBuild> builds;

    public static JenkinsBuildsMessageBuilder jenkinsBuilds() {
      return builder();
    }

  }

  @Value
  @Builder
  public static class GithubPrsMessage {

    List<GithubPr> prs;

    public static GithubPrsMessageBuilder githubPrs() {
      return builder();
    }

  }

  @Value
  @Builder
  public static class PortalChangeMessage {

    public enum Type {
      CREATED,
      REMOVED,
      UPDATED,
      EXECUTION,
    }

    Integer portalId;
    Type type;
    State state;

    public static PortalChangeMessageBuilder portalChange() {
      return builder();
    }

  }

  @Value
  @Builder
  public static class LogLineMessage {

    Integer portalId;
    String line;

    public static LogLineMessageBuilder logLine() {
      return builder();
    }

  }

  @Value
  @Builder
  public static class FirstSubscriberMessage {

    String topic;

    public static FirstSubscriberMessageBuilder firstSubscriber() {
      return builder();
    }

  }

  @Value
  @Builder
  public static class LastSubscriberMessage {

    String topic;

    public static LastSubscriberMessageBuilder lastSubscriber() {
      return builder();
    }

  }

}
