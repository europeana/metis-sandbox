package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.InputStream;


public interface HarvestService {

  /**
   * Harvest the given OAI endpoint with the given datasetId, data for the records and OAI-PMH data
   *
   * @param datasetId              The id of the dataset to be harvested
   * @param recordDataEncapsulated The encapsulation of data to be used to harvest each record
   * @param oaiHarvestData         The object that encapsulate the necessary data for harvesting
   * @param stepSize
   */
  void harvestOaiPmh(String datasetId, Record.RecordBuilder recordDataEncapsulated, OaiHarvestData oaiHarvestData,
                     Integer stepSize);

  /**
   * Harvest the input stream {@link InputStream} with the given datasetId and data of the records
   *
   * @param inputStream            The input stream to harvest from
   * @param datasetId              The id of the dataset to be harvested
   * @param recordDataEncapsulated The encapsulation of data to be used to harvest each record
   * @param stepSize               The step size to apply in the record selection
   * @throws HarvesterException In case an issue occurs while harvesting
   */
  void harvest(InputStream inputStream, String datasetId, Record.RecordBuilder recordDataEncapsulated, Integer stepSize) throws HarvesterException;
}
