package eu.europeana.metis.sandbox.service.record;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
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
   * @param contentTier the content tier value to update with
   * @param metadataTier the metadata tier value to update with
   */
  void setContentTierAndMetadataTier(Record recordToUpdate, MediaTier contentTier, MetadataTier metadataTier);

  /**
   * Deletes all records that are associated to a given dataset id
   * @param datasetId The id of the dataset
   */
  void remove(String datasetId);

}
