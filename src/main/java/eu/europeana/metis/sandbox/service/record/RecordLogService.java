package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;

public interface RecordLogService {

  /**
   * Persist the record event to keep it as a log
   *
   * @param recordEvent must not be null
   * @throws NullPointerException if event record is null
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void logRecordEvent(Event recordEvent);

  String getProviderRecordString(RecordIdType recordIdType, String recordId,
      String datasetId);

  RecordLogEntity getRecordLogEntity(RecordIdType recordIdType, String recordId,
      String datasetId);

  /**
   * Remove records matching the provided dataset id
   *
   * @param datasetId must not be null
   * @throws NullPointerException if dataset id is null
   * @throws ServiceException if there is an issue removing the dataset
   */
  void remove(String datasetId);
}
