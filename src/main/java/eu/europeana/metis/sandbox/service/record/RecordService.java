package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.domain.Record;

public interface RecordService {

  /**
   * Updates record's europeana id and provider id values
   *
   * @param recordToUpdate the record to update
   */
  void setEuropeanaIdAndProviderId(Record recordToUpdate);

  /**
   * Updates record's content tier and metadata tier values
   *
   * @param recordToUpdate the record to update
   * @param tierResults the results to update the given record with
   */
  void setTierResults(Record recordToUpdate, TierResults tierResults);

  /**
   * Deletes all records that are associated to a given dataset id
   * @param datasetId The id of the dataset
   */
  void remove(String datasetId);

}
