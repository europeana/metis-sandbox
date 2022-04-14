package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordProblemPatternRepository extends JpaRepository<RecordProblemPattern, Integer> {

  RecordProblemPattern findByExecutionPoint(ExecutionPoint executionPoint);

}
