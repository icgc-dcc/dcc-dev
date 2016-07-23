package org.icgc.dcc.dev.message;

import org.icgc.dcc.dev.jenkins.JenkinsBuild;
import org.icgc.dcc.dev.message.Messages.LogMessage;
import org.icgc.dcc.dev.slack.SlackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.val;

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
  SlackService slack;

  public void sendMessage(Object message) {
    if (message instanceof LogMessage) {
      val logMessage = (LogMessage) message;
      sendWebSocketMessage("/logs", logMessage);
    } else if (message instanceof JenkinsBuild) {
      val build = (JenkinsBuild) message;
      sendWebSocketMessage("/builds", build);
    }
  }

  private void sendWebSocketMessage(String destination, Object message) {
    messages.convertAndSend(topicPrefix + destination, message);
  }

}
