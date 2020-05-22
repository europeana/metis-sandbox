package eu.europeana.metis.sandbox.common.exception;

import static java.lang.String.format;

public class DatasetIndexRemoveException extends ServiceException {

  private static final long serialVersionUID = -1884956234091591447L;

  public DatasetIndexRemoveException(String datasetId, Throwable cause) {
    super(format("Error removing dataset id: [%s]. ", datasetId), cause);
  }
}
