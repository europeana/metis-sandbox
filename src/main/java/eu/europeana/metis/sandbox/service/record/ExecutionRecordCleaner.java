package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for cleaning execution records and associated entities.
 */
@Service
public class ExecutionRecordCleaner {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordWarningRepository executionRecordWarningRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  /**
   * Constructor.
   *
   * @param executionRecordRepository repository for managing execution records.
   * @param executionRecordWarningRepository repository for managing execution record warnings.
   * @param executionRecordErrorRepository repository for managing execution record errors.
   * @param executionRecordExternalIdentifierRepository repository for managing execution record external identifiers.
   * @param executionRecordTierContextRepository repository for managing execution record tier context entities.
   */
  public ExecutionRecordCleaner(ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordWarningRepository executionRecordWarningRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordWarningRepository = executionRecordWarningRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  /**
   * Removes execution records and associated entities for the given dataset ID.
   */
  @Transactional
  public void remove(String datasetId) {
    executionRecordWarningRepository.removeByExecutionRecord_Identifier_DatasetId(datasetId);
    executionRecordRepository.removeByIdentifier_DatasetId(datasetId);
    executionRecordErrorRepository.removeByIdentifier_DatasetId(datasetId);
    executionRecordExternalIdentifierRepository.removeByIdentifier_DatasetId(datasetId);
    executionRecordTierContextRepository.removeByIdentifier_DatasetId(datasetId);
  }
}
