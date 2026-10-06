package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

import lombok.experimental.StandardException;

/**
 * Exception thrown when an error occurs while removing indexed data for a specific dataset.
 */
@StandardException
public class DatasetIndexRemoveException extends ServiceException {

  public DatasetIndexRemoveException(String datasetId, Throwable cause) {
    super(format("Error removing dataset id: [%s]. ", datasetId), cause);
  }
}
