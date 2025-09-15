package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarning;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.StepStatisticProjection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRecordWarning} entities.
 */
@Repository
public interface ExecutionRecordWarningRepository extends
    JpaRepository<ExecutionRecordWarning, Long> {

  /**
   * Finds a list of ExecutionRecordWarningException entities based on the dataset ID and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the execution associated with the execution record.
   * @return A list of ExecutionRecordWarningException entities matching the given criteria.
   */
  List<ExecutionRecordWarning> findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
      String datasetId, String executionName);

  /**
   * Counts the number of entities based on the dataset ID and execution name.
   *
   * @param datasetId The ID of the dataset.
   * @param executionName The name of the execution associated with the entities.
   * @return The count of entities matching the specified dataset ID and execution name.
   */
  long countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(String datasetId,
      String executionName);

  /**
   * Retrieves statistics of execution steps, including the step name and the count of records grouped per step.
   *
   * @return A list of StepStatisticProjection containing the step name and the corresponding count.
   */
  @Query("""
      SELECT er.identifier.executionName AS step, COUNT(er) AS count 
            FROM ExecutionRecord er 
            GROUP BY er.identifier.executionName
      """)
  List<StepStatisticProjection> getStepStatistics();

  /**
   * Removes entities associated with the specified dataset ID.
   *
   * @param datasetId The ID of the dataset.
   */
  void removeByExecutionRecord_Identifier_DatasetId(String datasetId);
}
