package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

/**
 * The interface Execution point repository.
 */
@Repository
public interface ExecutionPointRepository extends JpaRepository<ExecutionPoint, Integer> {

  /**
   * Find by dataset id and execution step and execution timestamp execution point.
   *
   * @param datasetId the dataset id
   * @param executionName the execution step
   * @param executionTimestamp the execution timestamp
   * @return the execution point
   */
  ExecutionPoint findByDatasetIdAndExecutionNameAndExecutionTimestamp(String datasetId, String executionName, LocalDateTime executionTimestamp);

  /**
   * Find first by dataset id and execution step order by execution timestamp desc optional.
   *
   * @param datasetId the dataset id
   * @param executionName the execution step
   * @return the optional
   */
  Optional<ExecutionPoint> findFirstByDatasetIdAndExecutionNameOrderByExecutionTimestampDesc(String datasetId, String executionName);

  /**
   * Delete by dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  void deleteByDatasetId(String datasetId);

}
