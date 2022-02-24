package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.common.HarvestContent;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.springframework.web.multipart.MultipartFile;


public interface HarvestService {

  /**
   * Harvest the given file {@link MultipartFile} to a list of byte[], one string per file in the
   * zip
   *
   * @param file zip file containing one or more records
   * @return A HarvestContent object containing the content of the harvest and a bollean indicating
   * if it reached the max number of records
   * @throws ServiceException if file is not valid, error reading file, if records are empty
   */
  HarvestContent harvestZipMultipartFile(MultipartFile file) throws ServiceException;

  /**
   * Harvest the given URL {@link String} to a list of byte[], one string per file
   *
   * @param url URL for zip file containing one or more records
   * @return A HarvestContent object containing the content of the harvest and a bollean indicating
   * if it reached the max number of records
   * @throws ServiceException if error processing URL, if URL timeout, if records are empty
   */
  HarvestContent harvestZipUrl(String url) throws ServiceException;

  /**
   * Harvest the given OAI endpoint from the given event based on the given datasetId
   *
   * @param event The harvest OAI event
   * @param oaiRecordHeader
   * @param datasetId
   * @return A HarvestContent object containing the content of the harvest and a bollean indicating
   * if it reached the max number of records
   * @throws ServiceException if error processing endpoint, if endpoint timeout, if records are
   *                          empty
   */
  RecordInfo harvestOaiRecordHeader(OaiHarvestData oaiHarvestData, Record recordToHarvest,
      OaiRecordHeader oaiRecordHeader, String datasetIdd);
}
