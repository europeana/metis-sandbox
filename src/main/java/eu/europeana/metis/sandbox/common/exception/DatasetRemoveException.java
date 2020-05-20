package eu.europeana.metis.sandbox.common.exception;

import java.util.List;

public class DatasetRemoveException extends ServiceException {

  private static final long serialVersionUID = -1884956234091591447L;

  public DatasetRemoveException(String datasetId, Throwable cause) {
    super("Error removing dataset id: " + datasetId + ". " + cause.getMessage(), cause);
  }

  public DatasetRemoveException(List<String> datasetIds, Throwable cause) {
    super("Error removing dataset ids: " + datasetIds + ". " + cause.getMessage(), cause);
  }
}
