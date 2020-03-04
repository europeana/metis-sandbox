package eu.europeana.metis.sandbox.common.exception;

public class NonRecoverableServiceException extends ServiceException {

  public NonRecoverableServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
