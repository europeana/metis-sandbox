package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

public class RecordProcessingException extends ServiceException {

  private static final String MESSAGE = "There was an issue while processing record %s. Message: %s";

  private final String recordId;

  public RecordProcessingException(String recordId, Throwable cause) {
    super(format(MESSAGE, recordId, cause.getMessage()), cause);
    this.recordId = recordId;
  }

  public RecordProcessingException(String message, String recordId, Throwable cause) {
    super(format(MESSAGE, recordId, message), cause);
    this.recordId = recordId;
  }

  public String getRecordId() {
    return recordId;
  }
}
