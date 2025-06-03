package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProblemPatternDataCleaner {

    private final DatasetProblemPatternRepository datasetProblemPatternRepository;
    private final ExecutionPointRepository executionPointRepository;
    private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
    private final RecordProblemPatternRepository recordProblemPatternRepository;
    private final RecordTitleRepository recordTitleRepository;
    public ProblemPatternDataCleaner(DatasetProblemPatternRepository datasetProblemPatternRepository,
                                     ExecutionPointRepository executionPointRepository,
                                     RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository,
                                     RecordProblemPatternRepository recordProblemPatternRepository,
                                     RecordTitleRepository recordTitleRepository) {
        this.datasetProblemPatternRepository = datasetProblemPatternRepository;
        this.executionPointRepository = executionPointRepository;
        this.recordProblemPatternOccurrenceRepository = recordProblemPatternOccurrenceRepository;
        this.recordProblemPatternRepository = recordProblemPatternRepository;
        this.recordTitleRepository = recordTitleRepository;
    }

    @Transactional
    public void remove(String datasetId){
        recordProblemPatternOccurrenceRepository.deleteByRecordProblemPatternExecutionPointDatasetId(datasetId);
        recordTitleRepository.deleteByExecutionPointDatasetId(datasetId);
        recordProblemPatternRepository.deleteByExecutionPointDatasetId(datasetId);
        datasetProblemPatternRepository.deleteByExecutionPointDatasetId(datasetId);
        executionPointRepository.deleteByDatasetId(datasetId);
    }
}
