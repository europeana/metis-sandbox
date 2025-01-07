package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface EnrichmentService {

  /**
   * Enrich given recordToEnrich
   *
   * @param recordToEnrich must not be null
   * @return {@link RecordInfo} containing enriched recordToEnrich
   * @throws NullPointerException      if recordToEnrich is null
   * @throws RecordProcessingException if there is an issue processing the recordToEnrich
   */
  RecordInfo enrich(Record recordToEnrich);
}
