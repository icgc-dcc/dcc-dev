package org.icgc.dcc.dev.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

public class Messages {

  @Data
  @Accessors(chain = true)
  public static abstract class PortalMessage {
    
    String portalId;
    
  }
  
  
  @Data
  @Accessors(chain = true)
  @EqualsAndHashCode(callSuper = true)
  public static class LogMessage extends PortalMessage {
    
    String line;
    
  }
  
}
