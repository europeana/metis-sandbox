package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

public class RecordValidationException extends RecordProcessingException {

  private final String nodeId;

  public RecordValidationException(String recordId, String nodeId, String message) {
    super(format("There was an issue while validating record %s in node %s. Message: %s",
        recordId, nodeId, message), recordId, null);
    this.nodeId = nodeId;
  }

  public String getNodeId() {
    return nodeId;
  }
}
