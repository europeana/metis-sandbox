package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPattern;
import eu.europeana.metis.sandbox.entity.problempatterns.DatasetProblemPatternId;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DatasetProblemPatternRepository extends JpaRepository<DatasetProblemPattern, DatasetProblemPatternId> {

  @Transactional
  @Modifying
  @Query("UPDATE DatasetProblemPattern dpp SET dpp.recordOccurences = dpp.recordOccurences + 1 WHERE dpp.datasetProblemPatternId = ?1")
  void updateCounter(DatasetProblemPatternId datasetProblemPatternId);

  DatasetProblemPattern findByDatasetProblemPatternId(DatasetProblemPatternId datasetProblemPatternId);

}
