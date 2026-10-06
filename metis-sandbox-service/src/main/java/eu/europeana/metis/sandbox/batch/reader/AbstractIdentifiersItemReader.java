package eu.europeana.metis.sandbox.batch.reader;

import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.file.CloseableIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvestingIterator;
import eu.europeana.metis.sandbox.common.batch.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import eu.europeana.metis.sandbox.common.exception.DatasetEmptyException;
import eu.europeana.metis.sandbox.common.exception.StepIsTooBigException;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
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

  private HarvestingIterator<T, T> harvestingIterator;
  private CloseableIterator<T> closeableIterator;
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
      harvestingIterator = getHarvestingIterator(harvestParametersEntity);
      closeableIterator = harvestingIterator.getCloseableIterator();

    } catch (RuntimeException ex) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, ex);
      throw ex;
    }
  }

  @Override
  public ExecutionRecordExternalIdentifier read() {
    if (closeableIterator == null) {
      return null;
    }

    while (closeableIterator.hasNext()) {
      if (selectedCount >= maxAllowedRecords) {
        recordLimitExceeded = true;
        break;
      }

      T identifier = normalizeIdentifier(closeableIterator.next());

      if (!isDeleted(identifier)) {
        currentIndex++;

        if (currentIndex % step == 0) {
          selectedCount++;

          ExecutionRecordExternalIdentifier executionRecordExternalIdentifier = new ExecutionRecordExternalIdentifier();
          executionRecordExternalIdentifier.setExecutionRun(executionRun);
          executionRecordExternalIdentifier.setExternalRecordId(convertToCanonicalIdentifier(identifier));
          executionRecordExternalIdentifier.setDerivedRecordId(extractStringIdentifier(identifier));
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

    // Finally, close resources
    //todo: https://europeana.atlassian.net/browse/MET-6889 To update so that we can properly close here without the need to check type.
    if (harvestingIterator instanceof OaiHarvestingIterator) {
      try {
        harvestingIterator.close();
      } catch (IOException e) {
        log.error("Error closing OAI-PMH harvesting iterator", e);
      }
    }
    try {
      closeableIterator.close();
    } catch (IOException e) {
      log.error("Error closing closeable iterator", e);
    }

    return stepExecution.getExitStatus();
  }

  private boolean isStepSizeBiggerThanDatasetSize() {
    return selectedCount == 0 && currentIndex > 0;
  }

  private int normalizeStepSize(Integer stepSize) {
    return (stepSize == null || stepSize <= 0) ? 1 : stepSize;
  }

  /**
   * Normalizes the given identifier, ensuring it adheres to a specific format or structure.
   * <p>For example, a relative identifier for a system path becomes absolute</p>
   *
   * @param identifier the identifier to be normalized
   * @return the normalized identifier
   */
  protected abstract T normalizeIdentifier(T identifier);

  /**
   * Creates and returns a harvesting iterator configured with the specified parameters.
   *
   * @param harvestParametersEntity the harvest parameters entity containing the configuration for the harvesting process
   * @return a harvesting iterator
   */
  protected abstract HarvestingIterator<T, T> getHarvestingIterator(HarvestParametersEntity harvestParametersEntity);

  /**
   * Extracts a string representation of the given identifier.
   *
   * @param identifier the identifier from which the string representation is extracted
   * @return the string representation of the given identifier
   */
  protected abstract String extractStringIdentifier(T identifier);

  /**
   * Converts the given identifier to its canonical form.
   * <p>For example, a partitioned file path is converted back to its real/canonical form</p>
   *
   * @param identifier the identifier to be converted to its canonical form
   * @return the canonical representation of the identifier
   */
  protected abstract String convertToCanonicalIdentifier(T identifier);

  /**
   * Determines if the specified identifier is marked as deleted.
   *
   * @param identifier the identifier to check for deletion status
   * @return true if the identifier is deleted, false otherwise
   */
  protected abstract boolean isDeleted(T identifier);

  /**
   * Retrieves the type of batch job associated with this reader.
   *
   * @return the {@link BatchJobType} representing the specific type of job handled by this reader
   */
  protected abstract BatchJobType getJobType();
}

