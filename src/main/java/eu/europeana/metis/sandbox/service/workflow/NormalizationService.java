package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;

public interface NormalizationService {

  /**
   * Normalize the provided record
   *
   * @param record must not be null
   * @return {@link Record} normalized record
   * @throws NullPointerException if record is null
   * @throws RecordProcessingException if normalization fails
   */
  Record normalize(Record record);
}
