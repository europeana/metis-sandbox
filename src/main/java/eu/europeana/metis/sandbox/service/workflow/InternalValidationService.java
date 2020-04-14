package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;

public interface InternalValidationService {

  /**
   * Validates the given record against EDM-INTERNAL schema
   *
   * @param record must not be null
   * @return {@link Record} that is valid against EDM-INTERNAL schema
   * @throws NullPointerException      if record is null
   * @throws RecordValidationException if records fails at validation
   */
  Record validate(Record record);
}
