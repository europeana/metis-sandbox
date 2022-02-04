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
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record tier calculation view
   * @throws NoRecordFoundException if the record was not found
   */
  RecordTierCalculationView calculateTiers(String recordId, String datasetId) throws NoRecordFoundException;
}
