package eu.europeana.metis.sandbox.domain;

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
   */
  public RecordError(RecordProcessingException exception) {
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
