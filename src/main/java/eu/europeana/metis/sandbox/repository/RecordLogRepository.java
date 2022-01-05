package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RecordLogRepository extends JpaRepository<RecordLogEntity, Long> {

  /**
   * Get statistics by step for the given dataset id using a custom query
   *
   * @param datasetId must not be null
   * @return statistics for the given dataset
   * @see StepStatistic
   */
  @Query("SELECT " +
      "    new eu.europeana.metis.sandbox.entity.StepStatistic(rle.step, rle.status, COUNT(rle)) " +
      "FROM " +
      "    RecordLogEntity rle " +
      "WHERE rle.datasetId = ?1 " +
      "GROUP BY " +
      "    rle.step, rle.status")
  List<StepStatistic> getStepStatistics(String datasetId);

  /**
   * Get record given the provider record id, dataset id and step
   * @param recordId the record id
   * @param datasetId the dataset id
   * @param step the step
   * @return the record log
   */
  RecordLogEntity findRecordLogByRecordIdAndDatasetIdAndStep(String recordId, String datasetId, Step step);

  /**
   * Get record given the europeana record id, dataset id and step
   * @param europeanaId the record id
   * @param datasetId the dataset id
   * @param step the step
   * @return the record log
   */
  RecordLogEntity findRecordLogByEuropeanaIdAndDatasetIdAndStep(String europeanaId, String datasetId, Step step);

  /**
   * Delete records that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("delete from RecordLogEntity where datasetId = ?1")
  void deleteByDatasetId(String datasetId);
}
