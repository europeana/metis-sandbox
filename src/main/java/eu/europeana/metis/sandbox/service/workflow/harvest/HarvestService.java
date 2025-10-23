package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.HarvestException;
import eu.europeana.metis.sandbox.common.exception.StepIsTooBigException;
import eu.europeana.metis.sandbox.service.workflow.harvest.OaiHarvestService.HarvestFromIteratorResult;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Interface defining services for harvesting data from a specified target.
 *
 * @param <T> the type of identifier harvested.
 * @param <S> the type of harvest target.
 */
public interface HarvestService<T, S> {

  /**
   * Normalizes the provided step size by ensuring it is a positive number. If the input is null or less than or equal to 0, it
   * defaults to 1.
   *
   * @param stepSize the desired step size specified by the user; can be null or non-positive.
   * @return a normalized step size that is guaranteed to be a positive integer, with a default of 1.
   */
  default int normalizeStepSize(Integer stepSize) {
    return (stepSize == null || stepSize <= 0) ? 1 : stepSize;
  }

  /**
   * Harvests external identifiers from a specified harvest target. This method processes the target and retrieves a list of
   * identifiers, allowing for an optional step size to skip records in between steps. The step size is normalized to ensure a
   * valid positive value, defaulting to 1 if not specified or invalid.
   *
   * @param harvestTarget the target from which the identifiers will be harvested.
   * @param stepSize the size of the step for processing during the harvest. If null or less than or equal to zero, it defaults to
   * 1.
   * @return a {@code HarvestIdentifiersResult<T>} containing the harvested identifiers and a flag indicating whether the record
   * limit was exceeded during the process.
   */
  HarvestIdentifiersResult<T> harvestExternalIdentifiers(String datasetId, S harvestTarget, Integer stepSize);

  /**
   * Harvests a record from the specified harvest target, corresponding to the provided dataset ID and source record ID.
   *
   * @param harvestTarget the target object from which the record will be harvested. This can be an instance of a custom class or
   * object defining the data source details.
   * @param sourceRecordId the unique identifier of the source record to be harvested from the specified target.
   * @return a {@code HarvestedRecord} object containing the source record ID, harvested record ID, and its raw data content.
   * @throws HarvestException if an error occurs during the harvesting process, such as issues with accessing the target or
   * processing the record.
   */
  HarvestedRecord harvestRecord(String datasetId, S harvestTarget, String sourceRecordId) throws HarvestException;

  int getMaxAllowedRecords();

  default <R> HarvestFromIteratorResult harvestFromIterator(
      Iterable<R> iterator,
      Integer stepSize,
      Function<R, IterationResult> processor,
      Predicate<R> isDeleted
  ) throws HarvesterException {

    int step = normalizeStepSize(stepSize);
    int selectedCount = 0;
    int currentIndex = 0;
    int nextIndexToSelect = step - 1;

    boolean recordLimitExceeded = false;
    int maxRecords = getMaxAllowedRecords();

    for (R entry : iterator) {
      if (selectedCount >= maxRecords) {
        recordLimitExceeded = true;
        break;
      }

      if (currentIndex == nextIndexToSelect) {
        if (isDeleted.test(entry)) {
          nextIndexToSelect++; // skip deleted, pick next one
        } else {
          IterationResult result = processor.apply(entry);
          nextIndexToSelect += step;
          selectedCount++;
          if (result == IterationResult.TERMINATE) {
            break;
          }
        }
      }

      currentIndex++;
    }

    if (isStepSizeBiggerThanDatasetSize(selectedCount, currentIndex)) {
      throw new StepIsTooBigException(currentIndex);
    }

    return new HarvestFromIteratorResult(recordLimitExceeded);
  }

  private boolean isStepSizeBiggerThanDatasetSize(int datasetSize, int currentIndex) {
    return datasetSize == 0 && currentIndex > 0;
  }

}
