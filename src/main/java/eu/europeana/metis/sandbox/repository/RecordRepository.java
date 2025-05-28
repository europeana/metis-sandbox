package eu.europeana.metis.sandbox.repository;

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
   * Delete records that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("DELETE FROM RecordEntity WHERE datasetId = ?1")
  void deleteByDatasetId(String datasetId);

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
