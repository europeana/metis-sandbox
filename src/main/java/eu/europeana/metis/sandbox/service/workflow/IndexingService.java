package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;

public interface IndexingService {

  /**
   * Remove index for given dataset from publish
   *
   * @param datasetId must not be null
   * @throws NullPointerException if record is null
   * @throws DatasetIndexRemoveException if there is an issue removing the given datasets
   */
  void remove(String datasetId);
}
