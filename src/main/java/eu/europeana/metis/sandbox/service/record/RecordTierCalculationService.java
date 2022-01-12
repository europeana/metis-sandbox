package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.view.RecordTierCalculationView;
import eu.europeana.metis.sandbox.common.exception.NoRecordFoundException;

/**
 * Service that handles functionality for calculating record tiers.
 */
public interface RecordTierCalculationService {

  /**
   * Calculates the tiers for a provider record id and dataset id
   *
   * @param recordIdType the record id type
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record tier calculation view
   * @throws NoRecordFoundException if the record was not found
   */
  RecordTierCalculationView calculateTiers(RecordIdType recordIdType, String recordId, String datasetId)
      throws NoRecordFoundException;

  /**
   * The record id type.
   * <p>
   * This is used to identify provider identifier or europeana identifier representation for a record.
   * </p>
   */
  enum RecordIdType {
    PROVIDER_ID, EUROPEANA_ID
  }
}
