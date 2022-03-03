package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;

import java.io.InputStream;


public interface HarvestService {

  /**
   * Harvest the given OAI endpoint from the given event based on the given datasetId
   *
   * @param datasetId The id of the dataset the record to be harvested belongs to
   * @param oaiHarvestData  The object that encapsulate the necessary data for harvesting
   * @param recordToHarvest The encapsulation of the data of a record to be harvested
   * @return A HarvestContent object containing the content of the harvest and a bollean indicating
   * if it reached the max number of records
   * @throws ServiceException if error processing endpoint, if endpoint timeout, if records are
   *                          empty
   */
  RecordInfo harvestOaiRecordHeader(String datasetId, OaiHarvestData oaiHarvestData, Record.RecordBuilder recordToHarvest);

  void harvestOaiPmh(String datasetName, String datasetId,
                     Country country, Language language, OaiHarvestData oaiHarvestData);

  void harvest(InputStream inputStream, String datasetId, Record.RecordBuilder recordToHarvest) throws HarvesterException;
}
