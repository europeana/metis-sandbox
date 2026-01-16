package eu.europeana.metis.sandbox.common.exception;

import java.io.Serial;

public class XsltProcessingException extends RuntimeException{

  @Serial
  private static final long serialVersionUID = -1308555888429284944L;

  public XsltProcessingException(String message, Throwable cause) {
    super(message, cause);
  }

}
