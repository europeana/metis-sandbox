package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionPointRepository extends JpaRepository<ExecutionPoint, Integer> {

  ExecutionPoint findByDatasetIdAndExecutionStepAndExecutionTimestamp(String datasetId, String executionStep, LocalDateTime executionTimestamp);

}
