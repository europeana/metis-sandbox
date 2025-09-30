package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordConverter;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.Execution;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
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

  private final ExecutionRepository executionRepository;
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  /**
   * Constructor.
   *
   * @param executionRecordRepository The repository for managing ExecutionRecord entities.
   * @param executionRecordErrorRepository The repository for managing ExecutionRecordException entities.
   * @param executionRecordTierContextRepository The repository for managing tier contexts of execution records.
   */
  @Autowired
  public ExecutionRecordDTOItemWriter(ExecutionRepository executionRepository,
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.executionRepository = executionRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  @Override
  public void write(Chunk<? extends AbstractExecutionRecordDTO> chunk) {
    log.debug("BEGIN -> Write chunk");

    String datasetId = chunk.getItems().getFirst().getDatasetId();
    String executionId = chunk.getItems().getFirst().getExecutionId();
    String executionName = chunk.getItems().getFirst().getExecutionName();
    Execution execution = executionRepository.getByDatasetIdAndExecutionIdAndExecutionName(datasetId, executionId, executionName);

    ResultBucket resultBucket = processChunk(chunk, execution);
    persistResults(resultBucket);

    log.debug("END -> Write chunk");
  }

  private ResultBucket processChunk(Chunk<? extends AbstractExecutionRecordDTO> chunk, Execution execution) {
    final List<ExecutionRecord> executionRecords = new ArrayList<>();
    final List<ExecutionRecordError> executionRecordErrors = new ArrayList<>();
    final List<ExecutionRecordTierContext> executionRecordTierContexts = new ArrayList<>();

    for (AbstractExecutionRecordDTO abstractExecutionRecordDTO : chunk) {
      switch (abstractExecutionRecordDTO) {
        case SuccessExecutionRecordDTO successExecutionRecordDTO -> {
          executionRecords.add(ExecutionRecordConverter.convertToExecutionRecord(successExecutionRecordDTO, execution));
          Optional<ExecutionRecordTierContext> executionRecordTierContext =
              ExecutionRecordConverter.convertToExecutionRecordTierContext(successExecutionRecordDTO, execution);
          executionRecordTierContext.ifPresent(executionRecordTierContexts::add);
        }
        case FailExecutionRecordDTO failExecutionRecordDTO -> executionRecordErrors.add(
            ExecutionRecordConverter.converterToExecutionRecordError(failExecutionRecordDTO, execution));
      }
    }
    return new ResultBucket(executionRecords, executionRecordErrors, executionRecordTierContexts);
  }

  private void persistResults(ResultBucket bucket) {
    log.debug("In writer before saveAll");
    executionRecordRepository.saveAll(bucket.executionRecords());
    executionRecordTierContextRepository.saveAll(bucket.executionRecordTierContexts());
    executionRecordErrorRepository.saveAll(bucket.executionRecordErrors());
  }

  private record ResultBucket(
      List<ExecutionRecord> executionRecords,
      List<ExecutionRecordError> executionRecordErrors,
      List<ExecutionRecordTierContext> executionRecordTierContexts) {

  }
}

