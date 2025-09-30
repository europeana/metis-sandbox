package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.StepStatisticProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRecordError} entities.
 */
@Repository
public interface ExecutionRecordErrorRepository extends
    JpaRepository<ExecutionRecordError, Long> {

  /**
   * Retrieves a list of ExecutionRecordException entities based on the provided dataset ID and executionRun name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the executionRun.
   * @return A list of ExecutionRecordException entities matching the specified dataset ID and executionRun name.
   */
  List<ExecutionRecordError> findByExecutionRun_DatasetIdAndExecutionRun_ExecutionName(String datasetId, String executionName);

  /**
   * Finds an ExecutionRecordException based on dataset ID, record ID, and executionRun name.
   *
   * @param datasetId The ID of the dataset.
   * @param recordId The ID of the record within the dataset.
   * @param executionName The name of the executionRun.
   * @return The matching ExecutionRecordException, or null if not found.
   */
  ExecutionRecordError findByExecutionRun_DatasetIdAndIdentifier_RecordIdAndExecutionRun_ExecutionName(String datasetId,
      String recordId, String executionName);

  /**
   * Counts the number of entries matching the specified dataset ID and executionRun name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the executionRun.
   * @return The count of matching entries.
   */
  long countByExecutionRun_DatasetIdAndExecutionRun_ExecutionName(String datasetId, String executionName);

  /**
   * Retrieves statistics of executionRun steps, including the step name and the count of records grouped per step.
   *
   * @return A list of StepStatisticProjection containing the step name and the corresponding count.
   */
  @Query("""
      SELECT ere.executionRun.executionName AS step, COUNT(ere) AS count
            FROM ExecutionRecordError ere
            GROUP BY ere.executionRun.executionName
      """)
  List<StepStatisticProjection> getStepStatistics();

  /**
   * Deletes all records associated with the specified dataset ID from the repository.
   *
   * @param datasetId The ID of the dataset.
   */
  void removeByExecutionRun_DatasetId(String datasetId);
}
