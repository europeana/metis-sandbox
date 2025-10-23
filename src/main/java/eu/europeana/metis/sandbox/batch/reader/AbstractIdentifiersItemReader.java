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
  private Iterator<T> iterator;
  private ExecutionRun executionRun;

  int step;
  int selectedCount;
  int currentIndex;
  int nextIndexToSelect;
  boolean recordLimitExceeded;

  protected AbstractIdentifiersItemReader(HarvestParameterService harvestParameterService,
      DatasetExecutionSetupService datasetExecutionSetupService, ExecutionRunRepository executionRunRepository) {
    this.harvestParameterService = harvestParameterService;
    this.datasetExecutionSetupService = datasetExecutionSetupService;
    this.executionRunRepository = executionRunRepository;
  }

  /**
   * Performs pre-step initialization. Harvests identifiers and stores them in a queue in memory.
   */
  @BeforeStep
  public void beforeStep() {
    try {
      // Initialize iteration settings
      step = normalizeStepSize(Integer.parseInt(stepSize));
      selectedCount = 0;
      currentIndex = 0;
      nextIndexToSelect = step - 1;
      recordLimitExceeded = false;

      HarvestParametersEntity harvestParametersEntity = harvestParameterService
          .getHarvestingParametersById(UUID.fromString(harvestParameterId))
          .orElseThrow();
      executionRun = executionRunRepository.getByDatasetIdAndExecutionIdAndExecutionName(
          datasetId, targetExecutionId, getJobType().name());
      iterator = getIterable(harvestParametersEntity, Integer.parseInt(stepSize)).iterator();

      if (!iterator.hasNext()) {
        throw new DatasetEmptyException("No identifiers found for dataset %s".formatted(datasetId));
      }

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

    //Iterate until the selected identifier is found
    while (iterator.hasNext()) {

      if (selectedCount >= maxAllowedRecords) {
        recordLimitExceeded = true;
        log.warn("Maximum number of records harvested for datasetId {} exceeded.", datasetId);
        datasetExecutionSetupService.updateRecordLimitExceeded(Integer.parseInt(datasetId));
        break;
      }

      T identifier = getIdentifierTransformer().apply(iterator.next());
      currentIndex++;
      if (currentIndex - 1 == nextIndexToSelect) {
        if (isDeleted(identifier)) {
          nextIndexToSelect++; // skip deleted
        } else {
          nextIndexToSelect += step;
          selectedCount++;

          ExecutionRecordExternalIdentifier executionRecordExternalIdentifier = new ExecutionRecordExternalIdentifier();
          executionRecordExternalIdentifier.setExecutionRun(executionRun);
          executionRecordExternalIdentifier.setExternalRecordId(extractStringIdentifier(identifier));
          executionRecordExternalIdentifier.setDeleted(false);
          return executionRecordExternalIdentifier;
        }
      }
    }

    if (isStepSizeBiggerThanDatasetSize(selectedCount, currentIndex)) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, new StepIsTooBigException(currentIndex));
    }

    return null;
  }

  private boolean isStepSizeBiggerThanDatasetSize(int datasetSize, int currentIndex) {
    return datasetSize == 0 && currentIndex > 0;
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

