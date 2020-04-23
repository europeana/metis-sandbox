package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface InternalValidationService {

  /**
   * Validates the given record against EDM-INTERNAL schema
   *
   * @param record must not be null
   * return {@link RecordInfo} containing record that is valid against EDM-INTERNAL schema
   * @throws NullPointerException      if record is null
   * @throws RecordValidationException if records fails at validation
   */
  RecordInfo validate(Record record);
}
