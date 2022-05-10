package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface ExecutionPointRepository extends JpaRepository<ExecutionPoint, Integer> {

  ExecutionPoint findByDatasetIdAndExecutionStepAndExecutionTimestamp(String datasetId, String executionStep, LocalDateTime executionTimestamp);

  Optional<ExecutionPoint> findFirstByDatasetIdAndExecutionStepOrderByExecutionTimestampDesc(String datasetId, String executionStep);

  @Modifying
  void deleteByDatasetId(String datasetId);

}
