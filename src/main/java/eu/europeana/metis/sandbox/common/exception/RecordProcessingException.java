package eu.europeana.metis.sandbox.common.exception;

public class RecordProcessingException extends NonRecoverableServiceException {

  private String recordId;

  public RecordProcessingException(String recordId, Throwable cause) {
    super("There was an issue while processing record " + recordId, cause);
    this.recordId = recordId;
  }

  public String getRecordId() {
    return recordId;
  }
}
