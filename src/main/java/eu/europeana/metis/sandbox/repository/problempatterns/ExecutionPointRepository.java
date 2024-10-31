package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

/**
 * The interface Execution point repository.
 */
public interface ExecutionPointRepository extends JpaRepository<ExecutionPoint, Integer> {

  /**
   * Find by dataset id and execution step and execution timestamp execution point.
   *
   * @param datasetId the dataset id
   * @param executionStep the execution step
   * @param executionTimestamp the execution timestamp
   * @return the execution point
   */
  ExecutionPoint findByDatasetIdAndExecutionStepAndExecutionTimestamp(String datasetId, String executionStep, LocalDateTime executionTimestamp);

  /**
   * Find first by dataset id and execution step order by execution timestamp desc optional.
   *
   * @param datasetId the dataset id
   * @param executionStep the execution step
   * @return the optional
   */
  Optional<ExecutionPoint> findFirstByDatasetIdAndExecutionStepOrderByExecutionTimestampDesc(String datasetId, String executionStep);

  /**
   * Delete by dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  void deleteByDatasetId(String datasetId);

}
