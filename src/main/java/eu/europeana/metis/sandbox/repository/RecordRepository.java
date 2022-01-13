package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

  /**
   * Get record given the provider record id and dataset id
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @return the record entity
   */
  RecordEntity findRecordEntityByIdAndDatasetId(Long recordId, String datasetId);

  /**
   * Get record given the europeana record id and dataset id
   *
   * @param europeanaId the record id
   * @param datasetId the dataset id
   * @return the record entity
   */
  RecordEntity findRecordEntityByEuropeanaIdAndDatasetId(String europeanaId, String datasetId);

  /**
   * Update record with new values for europeana id and provider id
   *
   * @param recordId the id of the record to update
   * @param europeanaId the europeana id value to update with
   * @param providerId the provider id value to update with
   */
  @Modifying
  @Query("UPDATE RecordEntity rec SET rec.europeanaId = ?2, rec.providerId = ?3 WHERE rec.id = ?1")
  void updateEuropeanaIdAndProviderId(String recordId, String europeanaId, String providerId);

}
