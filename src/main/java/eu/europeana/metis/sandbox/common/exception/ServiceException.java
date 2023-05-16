package eu.europeana.metis.sandbox.common.exception;

public class ServiceException extends RuntimeException {

  private static final long serialVersionUID = -1308555888429284944L;

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public ServiceException(String message) {
    super(message);
  }
}
