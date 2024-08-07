package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

  /**
   * Returns all records that are port of the given datasetId
   *
   * @param datasetId The id of the dataset to search for
   * @return A list of record entities that are part of the given dataset
   */
  List<RecordEntity> findByDatasetId(String datasetId);


  /**
   * Update record with new values for content tier and metadata tier
   *
   * @param recordId the id of the record to update
   * @param contentTier the content tier value to update with
   * @param metadataTier the metadata tier value to update with
   * @param contentTierBeforeLicenseCorrection the value of the content tier before license correction
   * @param metadataTierLanguage the value of the metadata tier related to Language class
   * @param metadataTierEnablingElements the value of the metadata tier related to Enabling Elements class
   * @param metadataTierContextualClasses the values of the metadata tier related to Contextual Classes class
   * @param license the type of license
   */
  @Modifying
  @Query("UPDATE RecordEntity rec SET rec.contentTier = ?2, rec.metadataTier = ?3, " +
          "rec.contentTierBeforeLicenseCorrection = ?4, " +
          "rec.metadataTierLanguage = ?5, rec.metadataTierEnablingElements = ?6," +
          "rec.metadataTierContextualClasses = ?7, rec.license = ?8 WHERE rec.id = ?1")
  void updateRecordWithTierResults(Long recordId, String contentTier, String metadataTier, String contentTierBeforeLicenseCorrection,
                                   String metadataTierLanguage, String metadataTierEnablingElements, String metadataTierContextualClasses,
                                   String license);

  /**
   * Delete records that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("DELETE FROM RecordEntity WHERE datasetId = ?1")
  void deleteByDatasetId(String datasetId);

  /**
   * Find record by provider id
   *
   * @param providerId the provider id value to search
   */
  @Query("SELECT re FROM RecordEntity re WHERE re.providerId = ?1 AND re.datasetId = ?2")
  RecordEntity findByProviderIdAndDatasetId(String providerId, String datasetId);

  /**
   * Find record by europeana id
   *
   * @param europeanaId the europeana identifier value to search
   */
  @Query("SELECT re FROM RecordEntity re WHERE re.europeanaId = ?1 AND re.datasetId = ?2")
  RecordEntity findByEuropeanaIdAndDatasetId(String europeanaId, String datasetId);


  /**
   * Get metrics by dataset for a given time using custom query
   *
   * @return metrics Dataset Statistics
   * @see DatasetStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.common.aggregation.DatasetStatistic(re.datasetId, COUNT(*)) "
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
