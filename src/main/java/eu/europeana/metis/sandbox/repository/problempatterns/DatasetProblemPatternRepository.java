package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternCompositeKey;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatasetProblemPatternRepository extends JpaRepository<DatasetProblemPattern, DatasetProblemPatternCompositeKey> {

  @Transactional
  @Modifying
  @Query("UPDATE DatasetProblemPattern dpp SET dpp.recordOccurrences = dpp.recordOccurrences + 1 WHERE dpp.datasetProblemPatternCompositeKey = ?1")
  void updateCounter(DatasetProblemPatternCompositeKey datasetProblemPatternCompositeKey);

  @Modifying
  void deleteByExecutionPointDatasetId(String datasetId);

}
