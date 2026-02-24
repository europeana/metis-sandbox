package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import org.springframework.stereotype.Component;

/**
 * Class which acts as a wrapper and contains all the repositories required for the problem patterns.
 */
@Component
public class ProblemPatternsRepositories {

  private final ExecutionPointRepository executionPointRepository;
  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  private final RecordTitleRepository recordTitleRepository;

  /**
   * Constructor with required parameters.
   *
   * @param executionPointRepository the execution point repository
   * @param datasetProblemPatternRepository the dataset problem pattern repository
   * @param datasetProblemPatternJdbcRepository the dataset problem pattern jdbc repository
   * @param recordProblemPatternRepository the record problem pattern repository
   * @param recordProblemPatternOccurrenceRepository the record problem pattern occurrence repository
   * @param recordTitleRepository the record title repository
   * @param recordTitleJdbcRepository the record title jdbc repository
   */
  public ProblemPatternsRepositories(ExecutionPointRepository executionPointRepository,
      DatasetProblemPatternRepository datasetProblemPatternRepository,
      RecordProblemPatternRepository recordProblemPatternRepository,
      RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository,
      RecordTitleRepository recordTitleRepository) {
    this.executionPointRepository = executionPointRepository;
    this.datasetProblemPatternRepository = datasetProblemPatternRepository;
    this.recordProblemPatternRepository = recordProblemPatternRepository;
    this.recordProblemPatternOccurrenceRepository = recordProblemPatternOccurrenceRepository;
    this.recordTitleRepository = recordTitleRepository;
  }

  public ExecutionPointRepository getExecutionPointRepository() {
    return executionPointRepository;
  }

  public DatasetProblemPatternRepository getDatasetProblemPatternRepository() {
    return datasetProblemPatternRepository;
  }

  public RecordProblemPatternRepository getRecordProblemPatternRepository() {
    return recordProblemPatternRepository;
  }

  public RecordProblemPatternOccurrenceRepository getRecordProblemPatternOccurrenceRepository() {
    return recordProblemPatternOccurrenceRepository;
  }

  public RecordTitleRepository getRecordTitleRepository() {
    return recordTitleRepository;
  }
}
