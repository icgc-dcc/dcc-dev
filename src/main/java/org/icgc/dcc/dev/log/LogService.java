package org.icgc.dcc.dev.log;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PreDestroy;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.icgc.dcc.dev.message.MessageService;
import org.icgc.dcc.dev.message.Messages.LogMessage;
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

  Map<File, Tailer> tailers = Maps.newConcurrentMap();
  ExecutorService executor = Executors.newCachedThreadPool();

  @Autowired
  MessageService messages;

  @Synchronized
  public void tail(File logFile) {
    if (!tailers.containsKey(logFile)) {
      log.info("Tailing {}...", logFile);
      val tailer = new Tailer(logFile, this.new LogListener(logFile));
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

  @RequiredArgsConstructor
  private class LogListener extends TailerListenerAdapter {

    final File logFile;

    @Override
    public void handle(String line) {
      log.info("{}: {}", logFile, line);
      val message =  new LogMessage().setLogFile(logFile).setLine(line);
      messages.sendMessage(message);
    }

  }

}