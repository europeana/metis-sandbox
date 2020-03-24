package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;

public interface ExternalValidationService {

  /**
   * Orders and validates the given record against EDM-EXTERNAL schema
   *
   * @param record must not be null
   * @return ordered record that is valid against EDM-EXTERNAL schema
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if records fails at ordering
   */
  Record validate(Record record);
}
