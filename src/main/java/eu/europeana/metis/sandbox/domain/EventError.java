package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Contains info about an error of processing an event
 */
public class EventError {

  private final String message;
  private final String stackTrace;

  public EventError(Exception exception) {
    requireNonNull(exception, "Exception must not be null");
    this.message = exception.getMessage();
    this.stackTrace = ExceptionUtils.getStackTrace(exception);
  }

  public EventError(String message, String stackTrace) {
    requireNonNull(message, "Message must not be null");
    requireNonNull(stackTrace, "StackTrace must not be null");
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
