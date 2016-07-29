package org.icgc.dcc.dev.server.portal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PortalPrNotFoundException extends RuntimeException {

  public PortalPrNotFoundException() {
    super();
  }

  public PortalPrNotFoundException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public PortalPrNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public PortalPrNotFoundException(String message) {
    super(message);
  }

  public PortalPrNotFoundException(Throwable cause) {
    super(cause);
  }
  
}
