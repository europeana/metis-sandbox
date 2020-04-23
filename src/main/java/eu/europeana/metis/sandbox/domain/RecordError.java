package eu.europeana.metis.sandbox.domain;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class RecordError {

  private final String message;
  private final String stackTrace;

  public RecordError(Exception exception) {
    this.message = exception.getMessage();
    this.stackTrace = ExceptionUtils.getStackTrace(exception);
  }

  public RecordError(String message, String stackTrace) {
    this.message = message;
    this.stackTrace = stackTrace;
  }

  public String getMessage() {
    return message;
  }

  public String getStackTrace() {
    return stackTrace;
  }
}
