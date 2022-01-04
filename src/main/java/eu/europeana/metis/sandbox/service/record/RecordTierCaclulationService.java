package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.view.RecordTierCalculationView;

public interface RecordTierCaclulationService {

  /**
   * Calculates the tiers for a provider record id and dataset id
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record tier calculation view
   */
  RecordTierCalculationView calculateTiers(String recordId, String datasetId);


  enum RecordIdType {
    PROVIDER_ID, EUROPEANA_ID
  }
}
