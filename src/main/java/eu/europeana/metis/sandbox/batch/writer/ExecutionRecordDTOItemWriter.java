package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordConverter;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link ItemWriter} for writing {@link AbstractExecutionRecordDTO} objects to corresponding repositories.
 *
 * <p>This class processes a chunk of {@link AbstractExecutionRecordDTO} instances and segregates them into
 * {@link ExecutionRecord}, {@link ExecutionRecordError} and adds optional {@link ExecutionRecordTierContext} entities based on
 * the type of {@link AbstractExecutionRecordDTO}.
 * <p>The segregated entities are then persisted using the respective repositories
 * {@link ExecutionRecordRepository}, {@link ExecutionRecordErrorRepository}, and {@link ExecutionRecordTierContextRepository}.
 *
 * <p>The class ensures that successful execution records and their tier contexts are stored appropriately,
 * while record exceptions are stored as failed records.
 */
@Slf4j
@StepScope
@Component
public class ExecutionRecordDTOItemWriter implements ItemWriter<AbstractExecutionRecordDTO> {

  private final ExecutionRunRepository executionRunRepository;
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  @Value("#{stepExecution.jobExecution.jobInstance.jobName}")
  private String jobName;
  @Value("#{jobParameters['datasetId']}")
  protected String datasetId;
  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['batchJobSubType']}")
  private String batchJobSubTypeString;

  private ExecutionRun executionRun;

  /**
   * Constructor.
   *
   * @param executionRunRepository the repository used to store execution runs
   * @param executionRecordRepository The repository for managing ExecutionRecord entities.
   * @param executionRecordErrorRepository The repository for managing ExecutionRecordException entities.
   * @param executionRecordTierContextRepository The repository for managing tier contexts of execution records.
   */
  @Autowired
  public ExecutionRecordDTOItemWriter(ExecutionRunRepository executionRunRepository,
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.executionRunRepository = executionRunRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  void beforeStepInitializeExecutionRun() {
    this.executionRun = executionRunRepository.getByDatasetIdAndExecutionIdAndExecutionName(
        datasetId, targetExecutionId,
        FullBatchJobType.validateAndGetFullBatchJobType(jobName, batchJobSubTypeString).name());
  }

  @Override
  public void write(@NotNull Chunk<? extends AbstractExecutionRecordDTO> chunk) {
    log.debug("BEGIN -> Write chunk");

    ResultEntitiesBucket resultEntitiesBucket = processChunk(chunk);
    persistResults(resultEntitiesBucket);

    log.debug("END -> Write chunk");
  }

  private ResultEntitiesBucket processChunk(Chunk<? extends AbstractExecutionRecordDTO> chunk) {
    final List<ExecutionRecord> executionRecords = new ArrayList<>();
    final List<ExecutionRecordError> executionRecordErrors = new ArrayList<>();
    final List<ExecutionRecordTierContext> executionRecordTierContexts = new ArrayList<>();

    for (AbstractExecutionRecordDTO abstractExecutionRecordDTO : chunk) {
      switch (abstractExecutionRecordDTO) {
        case SuccessExecutionRecordDTO successExecutionRecordDTO -> {
          executionRecords.add(ExecutionRecordConverter.convertToExecutionRecord(successExecutionRecordDTO, executionRun));
          Optional<ExecutionRecordTierContext> executionRecordTierContext =
              ExecutionRecordConverter.convertToExecutionRecordTierContext(successExecutionRecordDTO, executionRun);
          executionRecordTierContext.ifPresent(executionRecordTierContexts::add);
        }
        case FailExecutionRecordDTO failExecutionRecordDTO -> executionRecordErrors.add(
            ExecutionRecordConverter.converterToExecutionRecordError(failExecutionRecordDTO, executionRun));
      }
    }
    return new ResultEntitiesBucket(executionRecords, executionRecordErrors, executionRecordTierContexts);
  }

  private void persistResults(ResultEntitiesBucket resultEntitiesBucket) {
    log.debug("In writer before saveAll");
    executionRecordRepository.saveAll(resultEntitiesBucket.executionRecords());
    executionRecordTierContextRepository.saveAll(resultEntitiesBucket.executionRecordTierContexts());
    executionRecordErrorRepository.saveAll(resultEntitiesBucket.executionRecordErrors());
  }

  private record ResultEntitiesBucket(
      List<ExecutionRecord> executionRecords,
      List<ExecutionRecordError> executionRecordErrors,
      List<ExecutionRecordTierContext> executionRecordTierContexts) {
  }
}

