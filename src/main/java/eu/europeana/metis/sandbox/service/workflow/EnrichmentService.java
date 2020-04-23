package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface EnrichmentService {

  /**
   * Enrich given record
   *
   * @param record must not be null
   * @return {@link RecordInfo} containing enriched record
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if there is an issue processing the record
   */
  RecordInfo enrich(Record record);
}
