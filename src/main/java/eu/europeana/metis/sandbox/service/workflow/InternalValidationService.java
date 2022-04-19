package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

import java.time.LocalDateTime;

public interface InternalValidationService {

  /**
   * Validates the given record against EDM-INTERNAL schema
   *
   * @param record must not be null
   * @param timestamp the timestamp that validate has started
   * return {@link RecordInfo} containing record that is valid against EDM-INTERNAL schema
   * @throws NullPointerException      if record is null
   * @throws RecordValidationException if records fails at validation
   */
  RecordInfo validate(Record record, LocalDateTime timestamp);
}
