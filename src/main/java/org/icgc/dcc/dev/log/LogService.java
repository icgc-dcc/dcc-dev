package org.icgc.dcc.dev.log;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.icgc.dcc.dev.message.MessageService;
import org.icgc.dcc.dev.message.Messages.LogMessage;
import org.icgc.dcc.dev.portal.PortalFileSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LogService {

  Map<String, Tailer> tailers = Maps.newConcurrentMap();
  ExecutorService executor = Executors.newCachedThreadPool();

  @Autowired
  PortalFileSystem fileSystem;
  @Autowired
  MessageService messages;

  @Synchronized
  public void startTailing(String portalId) {
    if (!tailers.containsKey(portalId)) {
      val logFile = fileSystem.getLogFile(portalId);
      log.info("Tailing {}...", logFile);
      
      val tailer = new Tailer(logFile, this.new LogListener(portalId));
      tailers.put(portalId, tailer);
      
      executor.execute(tailer);
    }
  }

  @Synchronized
  public void stopTailing(String portalId) {
    val tailer = tailers.remove(portalId);
    if (tailer == null) {
      return;
    }

    tailer.stop();
  }

  @PreDestroy
  public void shutdown() {
    tailers.values().forEach(Tailer::stop);
  }

  @RequiredArgsConstructor
  private class LogListener extends TailerListenerAdapter {

    final String portalId;

    @Override
    public void handle(String line) {
      log.info("{}: {}", portalId, line);
      val message =  new LogMessage().setLine(line).setPortalId(portalId);
      messages.sendMessage(message);
    }

  }

}