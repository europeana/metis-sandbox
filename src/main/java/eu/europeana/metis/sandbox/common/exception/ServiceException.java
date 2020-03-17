package eu.europeana.metis.sandbox.common.exception;

public class ServiceException extends RuntimeException {

  public ServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
