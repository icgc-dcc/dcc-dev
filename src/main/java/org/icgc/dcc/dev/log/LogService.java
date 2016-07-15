package org.icgc.dcc.dev.log;

import java.io.File;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogService {

  Map<File, Tailer> tailers = Maps.newConcurrentMap();
  
  @Autowired
  SimpMessagingTemplate messages;

  @Synchronized
  public void tail(File log) {
    if (!tailers.containsKey(log)) {
      val tailer = new Tailer(log, this.new LogListener());
      tailer.run();
      
      tailers.put(log, tailer);
    }
  }
  
  @Synchronized
  public void stop(File log) {
    val tailer = tailers.remove(log);
    if (tailer == null) {
      return;
    }
    
    tailer.stop();
  }
  
  @PreDestroy
  public void shutdown() {
    tailers.values().forEach(Tailer::stop);
  }

  private class LogListener extends TailerListenerAdapter {
    
   @Override
    public void handle(String line) {
     log.info(line);
     messages.convertAndSend("/topic/log", line);
    } 
   
  }

}