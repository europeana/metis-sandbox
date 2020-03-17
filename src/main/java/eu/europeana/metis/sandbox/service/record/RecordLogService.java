package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;

public interface RecordLogService {

  /**
   * Persist the record state to keep it as a log
   *
   * @param record must not be null
   * @throws NullPointerException if record is null
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void logRecord(Record record);
}
