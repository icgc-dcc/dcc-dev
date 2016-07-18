package org.icgc.dcc.dev.log;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  ExecutorService executor = Executors.newCachedThreadPool();
  
  @Autowired
  SimpMessagingTemplate messages;

  @Synchronized
  public void tail(File logFile) {
    if (!tailers.containsKey(logFile)) {
      log.info("Tailing {}...", logFile);
      val tailer = new Tailer(logFile, this.new LogListener());
      executor.execute(tailer);
      
      tailers.put(logFile, tailer);
    }
  }
  
  @Synchronized
  public void stop(File logFile) {
    val tailer = tailers.remove(logFile);
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