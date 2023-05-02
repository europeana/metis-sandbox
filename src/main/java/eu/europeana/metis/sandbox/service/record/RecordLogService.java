package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.entity.RecordErrorLogEntity;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import java.util.Set;

/**
 * Service handling functionality regarding record logs and errors.
 */
public interface RecordLogService {

  /**
   * Persist the record event to keep it as a log
   *
   * @param recordRecordProcessEvent must not be null
   * @throws NullPointerException if event record is null
   * @throws ServiceException if any unhandled exception happens, exception will contain original exception
   */
  void logRecordEvent(RecordProcessEvent recordRecordProcessEvent);

  /**
   * Gets provider record string.
   *
   * @param recordId  the record id
   * @param datasetId the dataset id
   * @param step  the step name
   * @return the provider record string
   * @throws NoRecordFoundException the no record found exception
   */
  String getProviderRecordString(String recordId, String datasetId, String step) throws NoRecordFoundException;

  /**
   * Get a record log entity from the database
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @param step the step that we want to get the record state
   * @return the record log entity
   */
  RecordLogEntity getRecordLogEntity(String recordId, String datasetId, Step step);


  /**
   * Get a record log entities from the database
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @param steps the steps that we want to get the record state
   * @return the record log entities
   */
  public Set<RecordLogEntity> getRecordLogEntities(String recordId, String datasetId, Set<Step> steps);

  /**
   * Get a record error log entity from the database
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record error log entity
   */
  RecordErrorLogEntity getRecordErrorLogEntity(String recordId, String datasetId);

  /**
   * Remove records matching the provided dataset id
   *
   * @param datasetId must not be null
   * @throws NullPointerException if dataset id is null
   * @throws ServiceException if there is an issue removing the dataset
   */
  void remove(String datasetId);
}
