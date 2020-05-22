package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.DatasetIndexRemoveException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;

public interface RecordLogService {

  /**
   * Persist the record event to keep it as a log
   *
   * @param recordEvent must not be null
   * @throws NullPointerException if event record is null
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void logRecordEvent(Event recordEvent);

  /**
   * Remove records matching the provided dataset id
   *
   * @param datasetId must not be null
   * @throws NullPointerException if dataset id is null
   * @throws DatasetIndexRemoveException if there is an issue removing the dataset
   */
  void remove(String datasetId);
}
