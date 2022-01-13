package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.service.record.RecordTierCalculationService.RecordIdType;

public interface RecordService {

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
  RecordEntity getRecordEntity(RecordIdType recordIdType, String recordId,
      String datasetId);

}
