package org.icgc.dcc.dev.message;

import java.io.File;

import lombok.Data;
import lombok.experimental.Accessors;

public class Messages {

  @Data
  @Accessors(chain = true)
  public static class LogMessage {
    
    File logFile;
    String line;
    
  }
  
}
