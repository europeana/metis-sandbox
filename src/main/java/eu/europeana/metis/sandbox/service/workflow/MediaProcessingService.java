package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Record;

public interface MediaProcessingService {

  /**
   * Process media of the given record and store it in a persistent storage
   *
   * @param record must not be null
   * @return {@link Record}
   * @throws NullPointerException if record is null
   * @throws RecordProcessingException if record fails at media processing
   */
  Record processMedia(Record record);
}
