package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

/**
 * Represents an exception thrown while processing a record <br />
 * Contains the record id
 */
public class RecordProcessingException extends ServiceException {

  private static final long serialVersionUID = -3104290194914814439L;

  private final String recordId;

  public RecordProcessingException(String recordId, Throwable cause) {
    super(format("Record: %s Message: %s ", recordId, cause.getMessage()), cause);
    this.recordId = recordId;
  }

  /**
   * Constructor intended to be used by exceptions inheriting from this one. <br />
   * Gives the ability to provide a custom message
   * @param message
   * @param recordId
   * @param cause
   */
  protected RecordProcessingException(String message, String recordId, Throwable cause) {
    super(message, cause);
    this.recordId = recordId;
  }

  public String getRecordId() {
    return recordId;
  }
}
