package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import java.util.List;

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
   * Remove records matching the provided dataset ids
   *
   * @param datasetIds must not be null
   * @throws NullPointerException if dataset id list is null
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void removeByDatasetIds(List<String> datasetIds);
}
