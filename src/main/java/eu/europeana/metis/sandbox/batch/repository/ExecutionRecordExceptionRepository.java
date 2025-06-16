package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordException;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.StepStatisticProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing ExecutionRecordException entities.
 */
@Repository
public interface ExecutionRecordExceptionRepository extends
    JpaRepository<ExecutionRecordException, ExecutionRecordIdentifierKey> {

  /**
   * Retrieves a list of ExecutionRecordException entities based on the provided dataset ID and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the execution.
   * @return A list of ExecutionRecordException entities matching the specified dataset ID and execution name.
   */
  List<ExecutionRecordException> findByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);

  /**
   * Finds an ExecutionRecordException based on dataset ID, record ID, and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param recordId The ID of the record within the dataset.
   * @param executionName The name of the execution.
   * @return The matching ExecutionRecordException, or null if not found.
   */
  ExecutionRecordException findByIdentifier_DatasetIdAndIdentifier_RecordIdAndIdentifier_ExecutionName(String datasetId,
      String recordId, String executionName);

  /**
   * Counts the number of entries matching the specified dataset ID and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the execution.
   * @return The count of matching entries.
   */
  long countByIdentifier_DatasetIdAndIdentifier_ExecutionName(String datasetId, String executionName);

  /**
   * Retrieves statistics of execution steps, including the step name and the count of records grouped per step.
   *
   * @return A list of StepStatisticProjection containing the step name and the corresponding count.
   */
  @Query("""
      SELECT r.identifier.executionName AS step, COUNT(r) AS count 
            FROM ExecutionRecordException r 
            GROUP BY r.identifier.executionName
      """)
  List<StepStatisticProjection> getStepStatistics();

  /**
   * Deletes all records associated with the specified dataset ID from the repository.
   *
   * @param datasetId The ID of the dataset.
   */
  void removeByIdentifier_DatasetId(String datasetId);
}
