package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.IndexEnv;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface IndexingService {

  /**
   * Index given record in the given {@link IndexEnv}
   *
   * @param record must not be null
   * @param indexEnv must not be null
   * @return {@link RecordInfo} containing indexed record
   * @throws NullPointerException if record or index are null
   * @throws RecordProcessingException if there is an issue while indexing
   */
  RecordInfo index(Record record, IndexEnv indexEnv);
}
