package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

  /**
   * Update record with new values for europeana id and provider id
   *
   * @param recordId the id of the record to update
   * @param europeanaId the europeana id value to update with
   * @param providerId the provider id value to update with
   */
  @Modifying
  @Query("UPDATE RecordEntity rec SET rec.europeanaId = ?2, rec.providerId = ?3 WHERE rec.id = ?1")
  void updateEuropeanaIdAndProviderId(Long recordId, String europeanaId, String providerId);

  /**
   * Update record with new values for content tier and metadata tier
   *
   * @param recordId the id of the record to update
   * @param contentTier the content tier value to update with
   * @param metadataTier the metadata tier value to update with
   */
  @Modifying
  @Query("UPDATE RecordEntity rec SET rec.contentTier = ?2, rec.metadataTier = ?3 WHERE rec.id = ?1")
  void updateContentTierAndMetadataTier(Long recordId, String contentTier, String metadataTier);

  /**
   * Delete records that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("delete from RecordEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);

  /**
   * Find record by provider id
   *
   * @param providerId the provider id value to search
   */
  @Query("select re from RecordEntity re where re.providerId = ?1 and re.datasetId = ?2")
  RecordEntity findByProviderIdAndDatasetId(String providerId, String datasetId);

  /**
   * Get metrics by dataset for a given time using custom query
   *
   * @return metrics Dataset Statistics
   * @see DatasetStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic(re.datasetId, COUNT(re)) "
      + "FROM RecordEntity re "
      + "GROUP BY re.datasetId")
  List<DatasetStatistic> getMetricDatasetStatistics();

  /**
   * Find records by dataset id and content tier value
   *
   * @param datasetId The id of the dataset the records belong to
   * @param contentTier The value of the content tier to look for
   */
  List<RecordEntity> findTop10ByDatasetIdAndContentTierOrderByEuropeanaIdAsc(String datasetId, String contentTier);

  /**
   * Find records by dataset id and metadata tier value
   *
   * @param datasetId The id of the dataset the records belong to
   * @param metadataTier The value of the metadata tier to look for
   */
  List<RecordEntity> findTop10ByDatasetIdAndMetadataTierOrderByEuropeanaIdAsc(String datasetId, String metadataTier);

  /**
   * Count records by dataset id and content tier value
   *
   * @param datasetId The id of the dataset the records belong to
   * @param contentTier The value of the content tier to look for
   */
  @Query("SELECT COUNT(re) FROM RecordEntity re WHERE re.datasetId = ?1 AND re.contentTier = ?2")
  int getRecordWithDatasetIdAndContentTierCount(String datasetId, String contentTier);

  /**
   * Count records by dataset id and metadata tier value
   *
   * @param datasetId The id of the dataset the records belong to
   * @param metadataTier The value of the metadata tier to look for
   */
  @Query("SELECT COUNT(re) FROM RecordEntity re WHERE re.datasetId = ?1 AND re.metadataTier = ?2")
  int getRecordWithDatasetIdAndMetadataTierCount(String datasetId, String metadataTier);

}
