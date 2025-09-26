package eu.europeana.metis.sandbox.batch.reader;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifierKey;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.workflow.harvest.HarvestIdentifiersResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

/**
 * Abstract base class for creating ItemReader implementations that read external identifiers associated with dataset execution
 * records. This class is designed to simplify the creation of specific ItemReader implementations by providing a common structure
 * and shared behavior for identifier harvesting and processing.
 * <p>
 * Note: This class cannot be used in parallelization at the current state.
 *
 * @param <T> type of the identifiers to be harvested. This can be any type of object.
 */
@Slf4j
@StepScope
public abstract class AbstractIdentifiersItemReader<T> implements ItemReader<ExecutionRecordExternalIdentifier> {

  @Value("#{jobParameters['targetExecutionId']}")
  protected String targetExecutionId;
  @Value("#{jobParameters['harvestParameterId']}")
  protected String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  protected String datasetId;
  @Value("#{jobParameters['stepSize']}")
  protected String stepSize;

  protected final HarvestParameterService harvestParameterService;
  protected final DatasetExecutionSetupService datasetExecutionSetupService;
  private final Deque<T> identifiers = new ArrayDeque<>();

  protected AbstractIdentifiersItemReader(HarvestParameterService harvestParameterService,
      DatasetExecutionSetupService datasetExecutionSetupService) {
    this.harvestParameterService = harvestParameterService;
    this.datasetExecutionSetupService = datasetExecutionSetupService;
  }

  /**
   * Harvests identifiers and stores them in a queue in memory.
   *
   * @param stepExecution encapsulates the execution context of a step in a batch job, providing metadata and runtime information
   */
  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    try {
      HarvestParametersEntity harvestParametersEntity = harvestParameterService
          .getHarvestingParametersById(UUID.fromString(harvestParameterId))
          .orElseThrow();
      HarvestIdentifiersResult<T> harvestIdentifiersResult = doHarvest(harvestParametersEntity, Integer.parseInt(stepSize));
      identifiers.addAll(harvestIdentifiersResult.identifiers());
      if (harvestIdentifiersResult.recordLimitExceeded()) {
        datasetExecutionSetupService.updateRecordLimitExceeded(Integer.parseInt(datasetId));
      }
    } catch (RuntimeException ex) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, ex);
      throw ex;
    }
  }

  @Override
  public ExecutionRecordExternalIdentifier read() {
    T identifier = takeIdentifier();
    if (identifier == null) {
      return null;
    }
    ExecutionRecordExternalIdentifierKey executionRecordExternalIdentifierKey = new ExecutionRecordExternalIdentifierKey();
    executionRecordExternalIdentifierKey.setDatasetId(datasetId);
    executionRecordExternalIdentifierKey.setExecutionId(targetExecutionId);
    executionRecordExternalIdentifierKey.setExecutionName(getJobType().name());
    executionRecordExternalIdentifierKey.setSourceRecordId(extractStringIdentifier(identifier));

    ExecutionRecordExternalIdentifier executionRecordExternalIdentifier = new ExecutionRecordExternalIdentifier();
    executionRecordExternalIdentifier.setIdentifier(executionRecordExternalIdentifierKey);
    executionRecordExternalIdentifier.setDeleted(isDeleted(identifier));
    return executionRecordExternalIdentifier;
  }

  private synchronized T takeIdentifier() {
    return identifiers.pollFirst();
  }

  protected abstract HarvestIdentifiersResult<T> doHarvest(HarvestParametersEntity params, int stepSize);

  protected abstract String extractStringIdentifier(T identifier);

  protected abstract boolean isDeleted(T identifier);

  protected abstract BatchJobType getJobType();
}

