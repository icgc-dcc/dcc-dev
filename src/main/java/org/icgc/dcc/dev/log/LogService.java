package org.icgc.dcc.dev.log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogService {

  @Autowired
  SimpMessagingTemplate messagingTemplate;

  public void publish(String line) {
    log.info(line);
    messagingTemplate.convertAndSend("/topic/log", line);
  }

}