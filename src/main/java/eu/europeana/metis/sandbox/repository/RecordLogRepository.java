package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * A {@link JpaRepository} that stores records logs.
 * <p>A record log is a representation of a record per each step.</p>
 */
public interface RecordLogRepository extends JpaRepository<RecordLogEntity, Long> {

  /**
   * Get statistics by step for the given dataset id using a custom query
   *
   * @param datasetId must not be null
   * @return statistics for the given dataset
   * @see StepStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.common.aggregation.StepStatistic(rle.step, rle.status, COUNT(*)) "
      + "FROM RecordLogEntity rle "
      + "WHERE rle.recordId.datasetId = ?1 "
      + "GROUP BY rle.step, rle.status")
  List<StepStatistic> getStepStatistics(String datasetId);

  /**
   * Get metrics by step for a given time using custom query
   *
   * @return metrics Step Statistics
   * @see StepStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.common.aggregation.StepStatistic(rle.step, rle.status, COUNT(*)) "
      + "FROM RecordLogEntity rle "
      + "GROUP BY rle.step, rle.status")
  List<StepStatistic> getMetricStepStatistics();

  /**
   * Get record given a record id, dataset id and step.
   * <p>The record id will be searched against both {@link RecordLogEntity#getRecordId().getProviderId()} and {@link
   * RecordLogEntity#getRecordId().getEuropeanaId()} together with {@link RecordLogEntity#getStep()}.</p>
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @param step the step
   * @return the record log
   */
  @Query("SELECT rle FROM RecordLogEntity rle WHERE (rle.recordId.providerId = ?1 OR rle.recordId.europeanaId= ?1) " +
          "AND rle.recordId.datasetId = ?2 AND rle.step = ?3 ")
  RecordLogEntity findRecordLogByRecordIdDatasetIdAndStep(String recordId, String datasetId, Step step);


  /**
   * Get set of records given a record id, dataset id and steps.
   * <p>The record id will be searched against both {@link RecordLogEntity#getRecordId().getProviderId()} and {@link
   * RecordLogEntity#getRecordId().getEuropeanaId()} together with {@link RecordLogEntity#getStep()}.</p>
   *
   * @param recordId the record id
   * @param datasetId the dataset id
   * @param steps the steps
   * @return set of record logs
   */
  @Query("SELECT rle FROM RecordLogEntity rle WHERE (rle.recordId.providerId = ?1 OR rle.recordId.europeanaId= ?1) " +
      "AND rle.recordId.datasetId = ?2 AND rle.step IN ?3")
  Set<RecordLogEntity> findRecordLogByRecordIdDatasetIdAndStepIn(String recordId, String datasetId, Set<Step> steps);

  /**
   * Find record log by dataset id and step set.
   *
   * @param datasetId the dataset id
   * @param step the step
   * @return the set
   */
  @Query("SELECT rle FROM RecordLogEntity rle WHERE rle.recordId.datasetId = ?1 AND rle.step = ?2")
  Set<RecordLogEntity> findRecordLogByDatasetIdAndStep(String datasetId, Step step);

  /**
   * Delete records that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  void deleteByRecordIdDatasetId(String datasetId);
}
