package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

/**
 * Represents an exception thrown while validating a record <br />
 * Contains the record id and the node id
 */
public class RecordValidationException extends RecordProcessingException {

  private static final long serialVersionUID = -3148276453701960919L;

  public RecordValidationException(String message, String recordId, String nodeId) {
    super(format("Record: [%s] Node: [%s] Message: [%s] ",
        recordId, nodeId, message), recordId, message, null);
  }
}
