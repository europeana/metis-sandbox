package eu.europeana.metis.sandbox.domain;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Contains info about an error of processing a record
 */
public class RecordError {

  private final String message;
  private final String stackTrace;

  /**
   * Constructor, creates object from the provided exception
   *
   * @param exception must not be null
   * @throws NullPointerException if exception is null
   */
  public RecordError(RecordProcessingException exception) {
    requireNonNull(exception, "Exception must not be null");
    this.message = Optional.ofNullable(exception.getReportMessage())
        .orElse("No message. Report to service provider");
    this.stackTrace = ExceptionUtils.getStackTrace(exception);
  }

  /**
   * Constructor
   *
   * @param message    must not be null
   * @param stackTrace must not be null
   */
  public RecordError(String message, String stackTrace) {
    requireNonNull(message, "Message must not be null");
    requireNonNull(stackTrace, "Stack trace must not be null");
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
