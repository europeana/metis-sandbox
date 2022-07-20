package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPatternOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface RecordProblemPatternOccurrenceRepository extends JpaRepository<RecordProblemPatternOccurrence, Integer> {

    @Modifying
    void deleteByRecordProblemPatternExecutionPointDatasetId(String datasetId);

}
