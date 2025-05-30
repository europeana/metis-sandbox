package eu.europeana.metis.sandbox.service.record;

import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningExceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionRecordRemover {

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  public ExecutionRecordRemover(ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordWarningExceptionRepository = executionRecordWarningExceptionRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  @Transactional
  public void remove(String datasetId){
    executionRecordWarningExceptionRepository.removeByExecutionRecord_Identifier_DatasetId(datasetId);
    executionRecordRepository.removeByIdentifier_DatasetId(datasetId);
    executionRecordExceptionLogRepository.removeByIdentifier_DatasetId(datasetId);
    executionRecordExternalIdentifierRepository.removeByIdentifier_DatasetId(datasetId);
    executionRecordTierContextRepository.removeByIdentifier_DatasetId(datasetId);
  }
}
