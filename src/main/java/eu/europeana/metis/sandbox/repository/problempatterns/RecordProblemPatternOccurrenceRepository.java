package eu.europeana.metis.sandbox.repository.problempatterns;

import eu.europeana.metis.sandbox.entity.problempatterns.RecordProblemPatternOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * The interface Record problem pattern occurrence repository.
 */
public interface RecordProblemPatternOccurrenceRepository extends JpaRepository<RecordProblemPatternOccurrence, Integer> {

    /**
     * Delete by record problem pattern execution point dataset id.
     *
     * @param datasetId the dataset id
     */
    @Modifying
    @Query("DELETE FROM RecordProblemPatternOccurrence rppo WHERE "+
        " EXISTS (SELECT 1 FROM RecordProblemPattern rpp "
        + " WHERE rppo.recordProblemPattern.recordProblemPatternId = rpp.recordProblemPatternId "
        + " AND EXISTS (SELECT 1 FROM ExecutionPoint ep "
        + " WHERE ep.executionPointId = rpp.executionPoint.executionPointId"
        + " AND ep.datasetId= ?1))")
    void deleteByRecordProblemPatternExecutionPointDatasetId(String datasetId);

}
