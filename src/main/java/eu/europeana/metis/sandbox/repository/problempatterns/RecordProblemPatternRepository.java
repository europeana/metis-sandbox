package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.ExecutionPoint;
import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * The interface Record problem pattern repository.
 */
public interface RecordProblemPatternRepository extends JpaRepository<RecordProblemPattern, Integer> {

  /**
   * Find by execution point record problem pattern.
   *
   * @param executionPoint the execution point
   * @return the record problem pattern
   */
  RecordProblemPattern findByExecutionPoint(ExecutionPoint executionPoint);

  /**
   * Delete by execution point dataset id.
   *
   * @param datasetId the dataset id
   */
  @Modifying
  @Query("DELETE FROM RecordProblemPattern rpp WHERE EXISTS (SELECT 1 FROM ExecutionPoint ep "
      + "WHERE rpp.executionPoint.executionPointId = ep.executionPointId AND ep.datasetId = ?1)")
  void deleteByExecutionPointDatasetId(String datasetId);

}
