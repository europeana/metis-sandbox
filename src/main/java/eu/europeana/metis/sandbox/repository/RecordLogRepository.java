package eu.europeana.metis.sandbox.repository;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import java.util.List;
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
  @Query( value = "SELECT new eu.europeana.metis.sandbox.entity.StepStatistic(rle.step, rle.status, COUNT(rle)) "
      + "FROM RecordLogEntity rle "
      + "WHERE rle.recordId.datasetId = ?1 "
      + "GROUP BY rle.step, rle.status")
  List<StepStatistic> getStepStatistics(String datasetId);

  /**
   * Delete records that belong to the given dataset id
   *
   * @param datasetId must not be null
   */
  @Modifying
  @Query("delete from RecordLogEntity where recordId.datasetId = ?1")
  void deleteByDatasetId(String datasetId);

  RecordLogEntity findRecordLogByEuropeanaIdAndDatasetIdAndStep(String recordId, String datasetId, Step mediaProcess);

  RecordLogEntity findRecordLogByRecordIdAndDatasetIdAndStep(String recordId, String datasetId, Step mediaProcess);
}
