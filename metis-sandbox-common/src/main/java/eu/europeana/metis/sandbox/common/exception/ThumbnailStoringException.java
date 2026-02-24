package eu.europeana.metis.sandbox.common.exception;

import java.io.Serial;

public class ThumbnailStoringException extends ServiceException {

  @Serial
  private static final long serialVersionUID = 2426586813201920583L;

  public ThumbnailStoringException(String message, Throwable cause) {
    super(message, cause);
  }
}
