package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

public class RecordValidationException extends RecordProcessingException {

  private static final long serialVersionUID = -3148276453701960919L;

  private final String nodeId;

  public RecordValidationException(String message, String recordId, String nodeId) {
    super(format("Record: %s Node: %s. Message: %s ",
        recordId, nodeId, message), recordId, null);
    this.nodeId = nodeId;
  }

  public String getNodeId() {
    return nodeId;
  }
}
