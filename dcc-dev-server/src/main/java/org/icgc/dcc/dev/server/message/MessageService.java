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

import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.message.Messages.LogMessage;
import org.icgc.dcc.dev.server.message.Messages.PortalMessage;
import org.icgc.dcc.dev.server.slack.SlackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.NonNull;
import lombok.val;

/**
 * Responsible for routing messages from publishers to subscribers using varies publication channels.
 */
@Service
public class MessageService {

  /**
   * Configuration.
   */
  @Value("${message.topicPrefix}")
  String topicPrefix;

  /**
   * Dependencies.
   */
  @Autowired
  SimpMessagingTemplate messages;
  @Autowired
  ApplicationEventPublisher publisher;
  @Autowired
  SlackService slack;

  public void sendMessage(@NonNull Object message) {
    if (message instanceof PortalMessage) {
      val portalChangedMessage = (PortalMessage) message;
      sendWebSocketMessage("/portal", portalChangedMessage.getPortal());
    } else if (message instanceof LogMessage) {
      val logMessage = (LogMessage) message;
      sendWebSocketMessage("/logs/" + logMessage.getPortalId(), logMessage);
    } else if (message instanceof JenkinsBuild) {
      val build = (JenkinsBuild) message;
      publisher.publishEvent(build);
      sendWebSocketMessage("/builds", build);
    } else {
      publisher.publishEvent(message);
    }
  }

  private void sendWebSocketMessage(String destination, Object message) {
    messages.convertAndSend(topicPrefix + destination, message);
  }

}
