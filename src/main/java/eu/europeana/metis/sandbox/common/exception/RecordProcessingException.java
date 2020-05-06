package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

/**
 * Represents an exception thrown while processing a record <br />
 * Contains the record id
 */
public class RecordProcessingException extends ServiceException {

  private static final long serialVersionUID = -3104290194914814439L;

  private final String recordId;
  private final String reportMessage;

  public RecordProcessingException(String recordId, Throwable cause) {
    super(format("Record: %s Message: %s ", recordId, cause.getMessage()), cause);
    this.recordId = recordId;
    this.reportMessage = cause.getMessage();
  }

  /**
   * Constructor intended to be used by exceptions inheriting from this one. <br />
   * Gives the ability to provide a custom message
   * @param message of what failed
   * @param recordId that failed
   * @param reportMessage message for reporting purposes
   * @param cause of the exception
   */
  protected RecordProcessingException(String message, String recordId, String reportMessage, Throwable cause) {
    super(message, cause);
    this.recordId = recordId;
    this.reportMessage = reportMessage;
  }

  public String getRecordId() {
    return recordId;
  }

  public String getReportMessage() {
    return reportMessage;
  }
}
