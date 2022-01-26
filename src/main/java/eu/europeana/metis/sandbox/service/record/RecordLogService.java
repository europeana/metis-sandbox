package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;

/**
 * Service handling functionality regarding record logs and errors.
 */
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
   * Get provider record content as String from the database
   *
   * @param recordIdType the record id type
   * @param recordId the reocrd id
   * @param datasetId the dataset id
   * @return the record content
   * @throws NoRecordFoundException if the record was not found
   */
  String getProviderRecordString(RecordIdType recordIdType, String recordId,
      String datasetId) throws NoRecordFoundException;

  /**
   * Get record log entity from the database
   *
   * @param recordIdType the record id type
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record log entity
   */
  RecordLogEntity getRecordLogEntity(RecordIdType recordIdType, String recordId,
      String datasetId);

  RecordErrorLogEntity getRecordErrorLogEntity(RecordIdType recordIdType, String recordId,
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
