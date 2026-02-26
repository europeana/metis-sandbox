package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

import java.io.Serial;

/**
 * Exception indicating that the provided dataset identifier is invalid.
 */
public class InvalidDatasetException extends ServiceException {

  @Serial
  private static final long serialVersionUID = -3012337707750461711L;

  public InvalidDatasetException(String datasetId) {
    super(format("Provided dataset id: [%s] is not valid. ", datasetId), null);
  }
}
