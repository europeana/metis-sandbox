package eu.europeana.metis.sandbox.batch.reader;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import eu.europeana.metis.sandbox.common.exception.DatasetEmptyException;
import eu.europeana.metis.sandbox.common.exception.StepIsTooBigException;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
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
  @Value("${sandbox.dataset.max-size}")
  protected int maxAllowedRecords;

  protected final HarvestParameterService harvestParameterService;
  protected final DatasetExecutionSetupService datasetExecutionSetupService;
  protected final ExecutionRunRepository executionRunRepository;
  private ExecutionRun executionRun;

  private Iterator<T> iterator;
  private int step;
  private int selectedCount;
  private int currentIndex;
  private boolean recordLimitExceeded;

  protected AbstractIdentifiersItemReader(HarvestParameterService harvestParameterService,
      DatasetExecutionSetupService datasetExecutionSetupService, ExecutionRunRepository executionRunRepository) {
    this.harvestParameterService = harvestParameterService;
    this.datasetExecutionSetupService = datasetExecutionSetupService;
    this.executionRunRepository = executionRunRepository;
  }

  /**
   * Performs pre-step initialization.
   */
  @BeforeStep
  public void beforeStep() {
    try {
      // Initialize iteration settings
      step = normalizeStepSize(Integer.parseInt(stepSize));
      selectedCount = 0;
      currentIndex = 0;
      recordLimitExceeded = false;

      HarvestParametersEntity harvestParametersEntity = harvestParameterService
          .getHarvestingParametersById(UUID.fromString(harvestParameterId))
          .orElseThrow();
      executionRun = executionRunRepository.getByDatasetIdAndExecutionIdAndExecutionName(
          datasetId, targetExecutionId, getJobType().name());
      iterator = getIterable(harvestParametersEntity, Integer.parseInt(stepSize)).iterator();

    } catch (RuntimeException ex) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, ex);
      throw ex;
    }
  }

  @Override
  public ExecutionRecordExternalIdentifier read() {
    if (iterator == null) {
      return null;
    }

    while (iterator.hasNext()) {
      if (selectedCount >= maxAllowedRecords) {
        recordLimitExceeded = true;
        break;
      }

      T identifier = getIdentifierTransformer().apply(iterator.next());

      if (!isDeleted(identifier)) {
        currentIndex++;

        if (currentIndex % step == 0) {
          selectedCount++;

          ExecutionRecordExternalIdentifier executionRecordExternalIdentifier = new ExecutionRecordExternalIdentifier();
          executionRecordExternalIdentifier.setExecutionRun(executionRun);
          executionRecordExternalIdentifier.setExternalRecordId(extractStringIdentifier(identifier));
          executionRecordExternalIdentifier.setDeleted(false);

          return executionRecordExternalIdentifier;
        }
      }
    }

    return null;
  }

  /**
   * This method is executed after the step to perform cleanup and error handling.
   * <p>
   * It evaluates the results of the step execution and adjusts the execution status accordingly.
   * <p>
   * If the step size is too big compared to the dataset, or if no identifiers are found, it updates the dataset with the
   * corresponding error and marks the step as failed.
   * <p>
   * Additionally, if a record limit is exceeded, it logs a warning and updates the dataset with this status.
   *
   * @param stepExecution the context of the step execution, containing information about the executed step
   * @return the exit status of the step execution, indicating success or failure
   */
  @AfterStep
  public ExitStatus afterStep(StepExecution stepExecution) {
    // Fatal: step size too big
    if (isStepSizeBiggerThanDatasetSize()) {
      StepIsTooBigException stepIsTooBigException = new StepIsTooBigException(currentIndex);
      datasetExecutionSetupService.updateDatasetWithError(datasetId, stepIsTooBigException);
      log.error("Step failed: step size too big for dataset {}", datasetId, stepIsTooBigException);
      return ExitStatus.FAILED;
    }

    // Fatal: no identifiers found
    if (selectedCount == 0) {
      DatasetEmptyException datasetEmptyException = new DatasetEmptyException(
          "No identifiers found for dataset %s".formatted(datasetId));
      datasetExecutionSetupService.updateDatasetWithError(datasetId, datasetEmptyException);
      log.error("Step failed: no identifiers found for dataset {}", datasetId, datasetEmptyException);
      return ExitStatus.FAILED;
    }

    // Non-fatal: record limit exceeded
    if (recordLimitExceeded) {
      log.warn("Maximum number of records harvested for datasetId {} exceeded.", datasetId);
      datasetExecutionSetupService.updateRecordLimitExceeded(Integer.parseInt(datasetId));
    }

    return stepExecution.getExitStatus();
  }

  private boolean isStepSizeBiggerThanDatasetSize() {
    return selectedCount == 0 && currentIndex > 0;
  }

  private int normalizeStepSize(Integer stepSize) {
    return (stepSize == null || stepSize <= 0) ? 1 : stepSize;
  }

  protected abstract Function<T, T> getIdentifierTransformer();

  protected abstract Iterable<T> getIterable(HarvestParametersEntity params, int stepSize);

  protected abstract String extractStringIdentifier(T identifier);

  protected abstract boolean isDeleted(T identifier);

  protected abstract BatchJobType getJobType();
}

