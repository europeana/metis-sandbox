package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProblemPatternDataRemover {

    private final DatasetProblemPatternRepository datasetProblemPatternRepository;
    private final ExecutionPointRepository executionPointRepository;
    private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
    private final RecordProblemPatternRepository recordProblemPatternRepository;

    public ProblemPatternDataRemover(DatasetProblemPatternRepository datasetProblemPatternRepository,
                                     ExecutionPointRepository executionPointRepository,
                                     RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository,
                                     RecordProblemPatternRepository recordProblemPatternRepository) {
        this.datasetProblemPatternRepository = datasetProblemPatternRepository;
        this.executionPointRepository = executionPointRepository;
        this.recordProblemPatternOccurrenceRepository = recordProblemPatternOccurrenceRepository;
        this.recordProblemPatternRepository = recordProblemPatternRepository;
    }

    @Transactional
    public void removeProblemPatternDataFromDatasetId(String datasetId){
        recordProblemPatternOccurrenceRepository.deleteByRecordProblemPatternExecutionPointDatasetId(datasetId);
        recordProblemPatternRepository.deleteByExecutionPointDatasetId(datasetId);
        datasetProblemPatternRepository.deleteByExecutionPointDatasetId(datasetId);
        executionPointRepository.deleteByDatasetId(datasetId);
    }
}
