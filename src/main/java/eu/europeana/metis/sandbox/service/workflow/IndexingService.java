package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.DatasetRemoveException;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.List;

public interface IndexingService {

  /**
   * Index given record
   *
   * @param record must not be null
   * @return {@link RecordInfo} containing indexed record
   * @throws NullPointerException if record is null
   * @throws RecordProcessingException if there is an issue while indexing
   */
  RecordInfo index(Record record);

  /**
   * Remove index from given datasets
   *
   * @param datasetIds must not be null
   * @throws NullPointerException if record is null
   * @throws DatasetRemoveException if there is an issue removing the given datasets
   */
  void remove(List<String> datasetIds);
}
