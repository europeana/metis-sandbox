package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.IndexEnvironment;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface IndexingService {

  /**
   * Index given record in the given {@link IndexEnvironment}
   *
   * @param record must not be null
   * @param indexEnvironment must not be null
   * @return {@link RecordInfo} containing indexed record
   * @throws NullPointerException if record or index are null
   * @throws RecordProcessingException if there is an issue while indexing
   */
  RecordInfo index(Record record, IndexEnvironment indexEnvironment);

  /**
   * Remove index for given dataset from all {@link IndexEnvironment}
   *
   * @param datasetId must not be null
   * @throws NullPointerException if record is null
   * @throws DatasetIndexRemoveException if there is an issue removing the given datasets
   */
  void remove(String datasetId);
}
