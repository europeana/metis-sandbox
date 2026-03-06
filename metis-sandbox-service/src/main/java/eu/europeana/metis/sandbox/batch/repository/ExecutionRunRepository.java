package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link ExecutionRun} entities.
 */
@Repository
public interface ExecutionRunRepository extends JpaRepository<ExecutionRun, Long> {

  /**
   * Retrieves an Execution entity based on the provided dataset ID, execution ID, and executionName.
   *
   * @param datasetId the unique identifier of the dataset
   * @param targetExecutionId the unique identifier of the execution
   * @param executionName the executionName of the execution
   * @return the Execution entity matching the specified dataset ID, execution ID, and execution executionName, or null if no
   * matching entity is found
   */
  ExecutionRun getByDatasetIdAndExecutionIdAndExecutionName(String datasetId, String targetExecutionId, String executionName);

  /**
   * Retrieves an {@link ExecutionRun} entity based on the provided execution ID.
   *
   * @param executionId the unique identifier of the execution
   * @return the {@link ExecutionRun} entity matching the specified execution ID, or null if no matching entity is found
   */
  ExecutionRun findByExecutionId(String executionId);
}
