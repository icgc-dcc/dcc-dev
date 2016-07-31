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

import static com.google.common.collect.Multimaps.synchronizedSetMultimap;

import org.icgc.dcc.dev.server.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.server.message.Messages.ExecutionMessage;
import org.icgc.dcc.dev.server.message.Messages.FirstSubscriberMessage;
import org.icgc.dcc.dev.server.message.Messages.LastSubscriberMessage;
import org.icgc.dcc.dev.server.message.Messages.LogMessage;
import org.icgc.dcc.dev.server.message.Messages.StateMessage;
import org.icgc.dcc.dev.server.slack.SlackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for routing messages from publishers to subscribers using varies publication channels.
 */
@Slf4j
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
    if (message instanceof StateMessage) {
      val stateMessage = (StateMessage) message;
      sendWebSocketMessage("/portal/state", stateMessage);
    } else if (message instanceof ExecutionMessage) {
      val executionMessage = (ExecutionMessage) message;
      sendWebSocketMessage("/portal/execute", executionMessage);
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

  /**
   * Listens for topic state transitions.
   * <p>
   * Adds higher level events.
   */
  @Component
  class MessageTopicListener {

    /**
     * State.
     */
    final SetMultimap<String, String> topicSubscriptions = synchronizedSetMultimap(HashMultimap.create());

    @EventListener
    void handle(AbstractSubProtocolEvent event) {
      val headers = StompHeaderAccessor.wrap(event.getMessage());
      val topic = headers.getDestination();
      val sessionId = headers.getSessionId();
      val command = headers.getCommand();

      if (command == null) return;

      switch (command) {
      case SUBSCRIBE:
        subscribe(topic, sessionId);
        break;
      case UNSUBSCRIBE:
        unsubscribe(topic, sessionId);
        break;
      case DISCONNECT:
        disconnect(sessionId);
        break;
      default:
        break;
      }
    }

    private void subscribe(String topic, String sessionId) {
      log.info("Session {} subscribed to: {}", sessionId, topic);
      topicSubscriptions.put(topic, sessionId);

      if (isFirst(topic)) {
        publisher.publishEvent(new FirstSubscriberMessage(topic));
      }
    }

    private void unsubscribe(String topic, String sessionId) {
      log.info("Session {} unsubscribed from: {}", sessionId, topic);
      topicSubscriptions.remove(topic, sessionId);

      if (isLast(topic)) {
        publisher.publishEvent(new LastSubscriberMessage(topic));
      }
    }

    private void disconnect(String sessionId) {
      val iterator = topicSubscriptions.entries().iterator();
      while (iterator.hasNext()) {
        val entry = iterator.next();
        val topic = entry.getKey();
        if (entry.getValue().equals(sessionId)) {
          log.info("Session {} disconnected from: {}", sessionId, topic);
          iterator.remove();

          if (isLast(topic)) {
            publisher.publishEvent(new LastSubscriberMessage(topic));
          }
        }
      }
    }

    private boolean isFirst(String topic) {
      return getSessionCount(topic) == 1;
    }

    private boolean isLast(String topic) {
      return getSessionCount(topic) == 0;
    }

    private int getSessionCount(String topic) {
      return topicSubscriptions.get(topic).size();
    }

  }

}
