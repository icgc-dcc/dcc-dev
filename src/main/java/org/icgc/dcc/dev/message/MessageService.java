package org.icgc.dcc.dev.message;

import org.icgc.dcc.dev.message.Messages.LogMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.val;

@Service
public class MessageService {

  @Autowired
  SimpMessagingTemplate messages;
  
  public void sendMessage(Object message) {
    if (message instanceof LogMessage) {
      val logMessage = (LogMessage) message;
      messages.convertAndSend("/topic/log", logMessage);
      
    }
  }
  
}
