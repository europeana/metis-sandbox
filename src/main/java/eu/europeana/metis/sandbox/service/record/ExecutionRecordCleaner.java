package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class responsible for cleaning execution records and associated entities.
 */
@AllArgsConstructor
@Service
public class ExecutionRecordCleaner {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordWarningRepository executionRecordWarningRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  /**
   * Removes all execution records and associated entities for the specified dataset ID.
   *
   * @param datasetId the ID of the dataset whose associated records are to be removed.
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
