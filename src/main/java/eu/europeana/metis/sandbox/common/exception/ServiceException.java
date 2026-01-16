package eu.europeana.metis.sandbox.common.exception;

import java.io.Serial;

public class ServiceException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = -1308555888429284944L;

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceException(String message) {
    super(message);
  }
}
