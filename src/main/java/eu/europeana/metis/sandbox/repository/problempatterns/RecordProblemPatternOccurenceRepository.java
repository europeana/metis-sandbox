package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPatternOccurence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordProblemPatternOccurenceRepository extends
    JpaRepository<RecordProblemPatternOccurence, Integer> {

}
