package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternStatistic;
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
  void deleteByExecutionPointDatasetId(String datasetId);

  /**
   * Get metrics by problem pattern ocurrences for a given time using custom query
   *
   * @return metrics Dataset Problem Pattern Statistics
   * @see DatasetProblemPatternStatistic
   */
  @Query(value = "SELECT new eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternStatistic(dpp.datasetProblemPatternCompositeKey.patternId, SUM(dpp.recordOccurrences)) "
      + "FROM DatasetProblemPattern dpp "
      + "GROUP BY dpp.datasetProblemPatternCompositeKey.patternId")
  List<DatasetProblemPatternStatistic> getMetricProblemPatternStatistics();
}
