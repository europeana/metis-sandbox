package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.RecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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

}
