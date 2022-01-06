package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.view.RecordTierCalculationView;

public interface RecordTierCalculationService {

  /**
   * Calculates the tiers for a provider record id and dataset id
   *
   * @param recordIdType the record id type
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record tier calculation view
   */
  RecordTierCalculationView calculateTiers(RecordIdType recordIdType, String recordId, String datasetId);

  enum RecordIdType {
    PROVIDER_ID, EUROPEANA_ID
  }
}
