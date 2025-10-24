package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.HarvestException;

/**
 * Interface defining services for harvesting data from a specified target.
 *
 * @param <T> the type of identifier harvested.
 * @param <S> the type of harvest target.
 */
public interface HarvestService<T, S> {

  /**
   * Retrieves an iterable collection of identifiers for data records available for harvesting
   * from the specified target, based on the associated dataset.
   * <p>
   * The consumer of this method is responsible for closing the iterable collection.
   *
   * @param datasetId the unique identifier for the dataset from which the data will be harvested.
   * @param harvestTarget the target object representing the source of harvestable data.
   * @return an iterable collection of record identifiers available for harvesting.
   */
  Iterable<T> getIterableHarvestingIdentifiers(String datasetId, S harvestTarget);

  /**
   * Harvests a record from the specified harvest target, corresponding to the provided dataset ID and source record ID.
   *
   * @param datasetId the unique identifier for the dataset from which the data will be harvested.
   * @param harvestTarget the target object from which the record will be harvested. This can be an instance of a custom class or
   * object defining the data source details.
   * @param sourceRecordId the unique identifier of the source record to be harvested from the specified target.
   * @return a {@code HarvestedRecord} object containing the source record ID, harvested record ID, and its raw data content.
   * @throws HarvestException if an error occurs during the harvesting process, such as issues with accessing the target or
   * processing the record.
   */
  HarvestedRecord harvestRecord(String datasetId, S harvestTarget, String sourceRecordId) throws HarvestException;

}
