package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface NormalizationService {

  /**
   * Normalize the provided record
   *
   * @param recordToNormalize must not be null
   * @return {@link RecordInfo} containing record normalized
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if normalization fails
   */
  RecordInfo normalize(Record recordToNormalize);
}
