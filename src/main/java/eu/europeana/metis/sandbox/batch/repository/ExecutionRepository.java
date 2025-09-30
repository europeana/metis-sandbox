package eu.europeana.metis.sandbox.batch.repository;

import eu.europeana.metis.sandbox.batch.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link Execution} entities.
 */
@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {

  /**
   * Retrieves an Execution entity based on the provided dataset ID, execution ID,
   * and executionName.
   *
   * @param datasetId the unique identifier of the dataset
   * @param targetExecutionId the unique identifier of the execution
   * @param executionName the executionName of the execution
   * @return the Execution entity matching the specified dataset ID, execution ID, and execution executionName,
   *         or null if no matching entity is found
   */
  Execution getByDatasetIdAndExecutionIdAndExecutionName(String datasetId, String targetExecutionId, String executionName);
}
