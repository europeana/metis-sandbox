package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface ExternalValidationService {

  /**
   * Orders and validates the given record against EDM-EXTERNAL schema
   *
   * @param recordToValidate must not be null
   * @return {@link RecordInfo} containing ordered record that is valid against EDM-EXTERNAL schema
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if records fails at ordering
   * @throws RecordValidationException if records fails at validation
   */
  RecordInfo validate(Record recordToValidate);
}
