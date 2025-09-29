package eu.europeana.metis.sandbox.batch.writer;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordConverter;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.ExceptionInfoDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordError;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifierKey;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.common.exception.DuplicateIdException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
  public ExecutionRecordDTOItemWriter(ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  @Override
  public void write(Chunk<? extends AbstractExecutionRecordDTO> chunk) {
    log.debug("BEGIN -> Write chunk");

    String datasetId = chunk.getItems().getFirst().getDatasetId();
    String executionId = chunk.getItems().getFirst().getExecutionId();

    Set<String> existingRecordIds = fetchExistingDbRecordIds(datasetId, executionId, chunk);
    ResultBucket resultBucket = processChunk(chunk, existingRecordIds);
    persistResults(resultBucket);

    log.debug("END -> Write chunk");
  }

  private Set<String> fetchExistingDbRecordIds(String datasetId, String executionId,
      Chunk<? extends AbstractExecutionRecordDTO> chunk) {
    List<String> recordIds = chunk.getItems().stream()
                                  .map(AbstractExecutionRecordDTO::getRecordId)
                                  .toList();

    return executionRecordRepository
        .findByIdentifier_DatasetIdAndIdentifier_ExecutionIdAndIdentifier_RecordIdIn(datasetId, executionId, recordIds)
        .stream()
        .map(ExecutionRecord::getIdentifier)
        .map(ExecutionRecordIdentifierKey::getRecordId)
        .collect(Collectors.toSet());
  }

  private ResultBucket processChunk(Chunk<? extends AbstractExecutionRecordDTO> chunk,
      Set<String> existingRecordIds) {
    final List<ExecutionRecord> executionRecords = new ArrayList<>();
    final List<ExecutionRecordError> executionRecordErrors = new ArrayList<>();
    final List<ExecutionRecordTierContext> executionRecordTierContexts = new ArrayList<>();

    final Set<String> seenInChunk = new HashSet<>();

    for (AbstractExecutionRecordDTO abstractExecutionRecordDTO : chunk) {
      String recordId = abstractExecutionRecordDTO.getRecordId();
      boolean isDuplicate = !seenInChunk.add(recordId) || existingRecordIds.contains(recordId);

      if (isDuplicate) {
        executionRecordErrors.add(handleDuplicate(abstractExecutionRecordDTO));
      } else {
        switch (abstractExecutionRecordDTO) {
          case SuccessExecutionRecordDTO successExecutionRecordDTO -> {
            executionRecords.add(ExecutionRecordConverter.convertToExecutionRecord(successExecutionRecordDTO));
            Optional<ExecutionRecordTierContext> executionRecordTierContext =
                ExecutionRecordConverter.convertToExecutionRecordTierContext(successExecutionRecordDTO);
            executionRecordTierContext.ifPresent(executionRecordTierContexts::add);
          }
          case FailExecutionRecordDTO failExecutionRecordDTO -> executionRecordErrors.add(
              ExecutionRecordConverter.converterToExecutionRecordError(failExecutionRecordDTO));
        }
      }
    }
    return new ResultBucket(executionRecords, executionRecordErrors, executionRecordTierContexts);
  }

  private ExecutionRecordError handleDuplicate(AbstractExecutionRecordDTO abstractExecutionRecordDTO) {
    log.warn("Duplicate detected for recordId={} - inserting as error", abstractExecutionRecordDTO.getRecordId());
    FailExecutionRecordDTO fail = FailExecutionRecordDTO.createValidated(
        builder -> builder
            .datasetId(abstractExecutionRecordDTO.getDatasetId())
            .recordId(abstractExecutionRecordDTO.getRecordId())
            .externalRecordId(abstractExecutionRecordDTO.getExternalRecordId())
            .sourceRecordId(abstractExecutionRecordDTO.getSourceRecordId())
            .executionId(abstractExecutionRecordDTO.getExecutionId())
            .executionName(abstractExecutionRecordDTO.getExecutionName())
            .exceptionInfoDTO(ExceptionInfoDTO.from(new DuplicateIdException("Duplicate id detected")))
    );
    return ExecutionRecordConverter.converterToExecutionRecordError(fail);
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
      List<ExecutionRecordTierContext> executionRecordTierContexts) {}
}

