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

  /**
   * Inserts or updates the matching entity with the record occurrences incremented.
   *
   * <p>If a record already exists, then it increments the record occurrences by the specified amount.
   * <p>Note: This is a native query, so spring jpa persistent context might not be auto-updated.
   *
   * @param executionPointId the id of the execution point
   * @param patternId the id of the pattern
   * @param incrementValue the value to increment the record occurrences by
   * @return the updated record occurrences
   */
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

  /**
   * Projection interface for retrieving dataset problem pattern statistics.
   *
   * <p>Represents the aggregated results of problem pattern occurrences across datasets.
   * <p>Contains methods to retrieve the pattern ID and total occurrences.
   */
  interface DatasetProblemPatternStatisticProjection {

    String getPatternId();

    long getTotalOccurrences();
  }

}
