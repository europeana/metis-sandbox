package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProblemPatternDataRemoverTest {

    @Mock
    private DatasetProblemPatternRepository datasetProblemPatternRepository;

    @Mock
    private ExecutionPointRepository executionPointRepository;

    @Mock
    private RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;

    @Mock
    private RecordProblemPatternRepository recordProblemPatternRepository;

    @InjectMocks
    private ProblemPatternDataRemover problemPatternDataRemover;

    @Test
    void removeProblemPatternDataFromDatasetId_expectSuccess(){
        problemPatternDataRemover.removeProblemPatternDataFromDatasetId("1");
        verify(datasetProblemPatternRepository, times(1)).deleteByExecutionPointDatasetId("1");
        verify(executionPointRepository, times(1)).deleteByDatasetId("1");
        verify(recordProblemPatternOccurrenceRepository, times(1)).deleteByRecordProblemPatternExecutionPointDatasetId("1");
        verify(recordProblemPatternRepository, times(1)).deleteByExecutionPointDatasetId("1");

    }

}
