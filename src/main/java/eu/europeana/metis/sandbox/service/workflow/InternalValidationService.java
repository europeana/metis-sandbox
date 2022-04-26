package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordValidationException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

import java.time.LocalDateTime;
import java.util.Map;


public interface InternalValidationService {

  /**
   * Validates the given record against EDM-INTERNAL schema
   *
   * @param recordToValidate must not be null
   * return {@link RecordInfo} containing record that is valid against EDM-INTERNAL schema
   * @throws NullPointerException      if record is null
   * @throws RecordValidationException if records fails at validation
   */
  RecordInfo validate(Record recordToValidate);

  /**
   * Method void to clean up the mapping regarding execution timestamp.
   * Related to problem pattern analysis
   * @return A copy of the map that was just cleaned
   */
  Map<String, LocalDateTime> cleanMappingExecutionTimestamp();
}
