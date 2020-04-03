package eu.europeana.metis.sandbox.common.exception;

public class RecordProcessingException extends ServiceException {

  private final String recordId;

  public RecordProcessingException(String recordId, Throwable cause) {
    super("There was an issue while processing record " + recordId, cause);
    this.recordId = recordId;
  }

  public RecordProcessingException(String message, String recordId, Throwable cause) {
    super(message, cause);
    this.recordId = recordId;
  }

  public String getRecordId() {
    return recordId;
  }
}
