package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface RecordProblemPatternRepository extends JpaRepository<RecordProblemPattern, Integer> {

  RecordProblemPattern findByExecutionPoint(ExecutionPoint executionPoint);

  @Modifying
  void deleteByExecutionPointDatasetId(String datasetId);

}
