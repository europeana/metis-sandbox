package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

/**
 * Exception indicating that the provided dataset identifier is invalid.
 */
public class InvalidDatasetException extends ServiceException {

  public InvalidDatasetException(String datasetId) {
    super(format("Provided dataset id: [%s] is not valid. ", datasetId), null);
  }
}
