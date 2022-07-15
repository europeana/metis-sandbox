package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.entity.DatasetStatistic;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
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
   * Get metrics by step for a given time using custom query
   *
   * @return metrics Dataset Statistics
   * @see DatasetStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.entity.DatasetStatistic(re.datasetId, COUNT(re)) "
      + "FROM RecordEntity re "
      + "GROUP BY re.datasetId")
  List<DatasetStatistic> getMetricDatasetStatistics();
}
