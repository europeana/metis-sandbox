package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;

public interface ExternalValidationService {

  /**
   *
   * @param record
   * @return
   * @throws NullPointerException if record is null
   * @throws RecordProcessingException if records fails at validation
   */
  Record validate(Record record);
}
