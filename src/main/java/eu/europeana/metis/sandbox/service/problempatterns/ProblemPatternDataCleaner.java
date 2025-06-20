package eu.europeana.metis.sandbox.service.problempatterns;

import eu.europeana.metis.sandbox.repository.problempatterns.DatasetProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.ExecutionPointRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternOccurrenceRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordProblemPatternRepository;
import eu.europeana.metis.sandbox.repository.problempatterns.RecordTitleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A service class responsible for cleaning and removing problem pattern data.
 */
@Component
public class ProblemPatternDataCleaner {

  private final DatasetProblemPatternRepository datasetProblemPatternRepository;
  private final ExecutionPointRepository executionPointRepository;
  private final RecordProblemPatternOccurrenceRepository recordProblemPatternOccurrenceRepository;
  private final RecordProblemPatternRepository recordProblemPatternRepository;
  private final RecordTitleRepository recordTitleRepository;

  /**
   * Constructor.
   *
   * @param datasetProblemPatternRepository the repository for dataset problem patterns
   * @param executionPointRepository the repository for execution points
   * @param recordProblemPatternOccurrenceRepository the repository for record problem pattern occurrences
   * @param recordProblemPatternRepository the repository for record problem patterns
   * @param recordTitleRepository the repository for record titles
   */
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

  /**
   * Removes all problem pattern data associated with the specified dataset id.
   *
   * @param datasetId the dataset id to remove associated data for
   */
  @Transactional
  public void remove(String datasetId) {
    recordProblemPatternOccurrenceRepository.deleteByRecordProblemPatternExecutionPointDatasetId(datasetId);
    recordTitleRepository.deleteByExecutionPointDatasetId(datasetId);
    recordProblemPatternRepository.deleteByExecutionPointDatasetId(datasetId);
    datasetProblemPatternRepository.deleteByExecutionPointDatasetId(datasetId);
    executionPointRepository.deleteByDatasetId(datasetId);
  }

  /**
   * Deletes all problem pattern data from the system.
   */
  @Transactional
  public void deleteAll() {
    recordTitleRepository.deleteAll();
    recordProblemPatternOccurrenceRepository.deleteAll();
    recordProblemPatternRepository.deleteAll();
    datasetProblemPatternRepository.deleteAll();
    executionPointRepository.deleteAll();
  }
}
