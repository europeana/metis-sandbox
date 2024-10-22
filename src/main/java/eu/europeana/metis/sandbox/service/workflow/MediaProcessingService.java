package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface MediaProcessingService {

  /**
   * Process media of the given record and store it in a persistent storage
   *
   * @param recordToProcess must not be null
   * @return {@link RecordInfo} containing record with processed media
   * @throws NullPointerException      if record is null
   * @throws RecordProcessingException if record fails at media processing
   */
  RecordInfo processMedia(Record recordToProcess);
}
