package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;

public interface EnrichmentService {

  /**
   * Enrich given record
   *
   * @param record must not be null
   * @return record enriched
   * @throws NullPointerException if record is null
   * @throws RecordProcessingException if there is an issue processing the record
   */
  Record enrich(Record record);
}
