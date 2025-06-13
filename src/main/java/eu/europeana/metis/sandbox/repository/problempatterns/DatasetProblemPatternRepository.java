package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Jpa repository for {@link DatasetProblemPattern}
 */
@Repository
public interface DatasetProblemPatternRepository extends JpaRepository<DatasetProblemPattern, DatasetProblemPatternCompositeKey> {

  /**
   * Delete row by provided dataset id
   *
   * @param datasetId the dataset id
   */
  @Modifying
  @Query("""
      DELETE FROM DatasetProblemPattern dpp WHERE EXISTS (
            SELECT 1 FROM ExecutionPoint ep WHERE dpp.executionPoint.executionPointId = ep.executionPointId AND ep.datasetId = :datasetId)
      """)
  void deleteByExecutionPointDatasetId(@Param("datasetId") String datasetId);

  @Query("""
      SELECT dpp.datasetProblemPatternCompositeKey.patternId AS patternId,
            SUM(dpp.recordOccurrences) AS totalOccurrences
            FROM DatasetProblemPattern dpp
            GROUP BY dpp.datasetProblemPatternCompositeKey.patternId
      """)
  List<DatasetProblemPatternStatisticProjection> getProblemPatternStatistics();

  @Query(value = """
          INSERT INTO problem_patterns.dataset_problem_pattern (execution_point_id, pattern_id, record_occurrences)
          VALUES (:executionPointId, :patternId, :incrementValue)
          ON CONFLICT (execution_point_id, pattern_id)
          DO UPDATE SET record_occurrences = problem_patterns.dataset_problem_pattern.record_occurrences + :incrementValue
          RETURNING record_occurrences
      """, nativeQuery = true)
  Integer upsertCounter(
      @Param("executionPointId") int executionPointId,
      @Param("patternId") String patternId,
      @Param("incrementValue") int incrementValue);

  interface DatasetProblemPatternStatisticProjection {

    String getPatternId();

    long getTotalOccurrences();
  }

}
