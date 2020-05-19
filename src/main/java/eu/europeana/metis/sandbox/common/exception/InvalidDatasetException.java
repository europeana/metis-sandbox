package eu.europeana.metis.sandbox.common.exception;

public class InvalidDatasetException extends ServiceException {

  private static final long serialVersionUID = -3012337707750461711L;

  public InvalidDatasetException(String datasetId) {
    super("Provided dataset id is not valid: " + datasetId, null);
  }
}
