package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

public class RecordProcessingException extends ServiceException {

  private static final long serialVersionUID = -3104290194914814439L;

  private final String recordId;

  public RecordProcessingException(String recordId, Throwable cause) {
    super(format("Record: %s. Message: %s ", recordId, cause.getMessage()), cause);
    this.recordId = recordId;
  }

  protected RecordProcessingException(String message, String recordId, Throwable cause) {
    super(message, cause);
    this.recordId = recordId;
  }

  public String getRecordId() {
    return recordId;
  }
}
