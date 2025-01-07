package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import eu.europeana.metis.sandbox.common.aggregation.DatasetProblemPatternStatistic;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Jpa repository for {@link DatasetProblemPattern}
 */
public interface DatasetProblemPatternRepository extends JpaRepository<DatasetProblemPattern, DatasetProblemPatternCompositeKey> {

  /**
   * Delete row by provided dataset id
   * @param datasetId the dataset id
   */
  @Modifying
  @Query("DELETE FROM DatasetProblemPattern dpp WHERE EXISTS ("
      + " SELECT 1 FROM ExecutionPoint ep WHERE dpp.executionPoint.executionPointId = ep.executionPointId AND ep.datasetId = ?1)")
  void deleteByExecutionPointDatasetId(String datasetId);

  /**
   * Get metrics by problem pattern occurrences for a given time using custom query
   *
   * @return metrics Dataset Problem Pattern Statistics
   * @see DatasetProblemPatternStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.common.aggregation.DatasetProblemPatternStatistic("
      + "dpp.datasetProblemPatternCompositeKey.patternId, SUM(dpp.recordOccurrences)) "
      + "FROM DatasetProblemPattern dpp "
      + "GROUP BY dpp.datasetProblemPatternCompositeKey.patternId")
  List<DatasetProblemPatternStatistic> getMetricProblemPatternStatistics();
}
