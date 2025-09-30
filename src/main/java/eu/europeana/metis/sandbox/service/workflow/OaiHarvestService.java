package eu.europeana.metis.sandbox.service.workflow;

import static java.util.Optional.ofNullable;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.experimental.StandardException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for harvesting records from an OAI-PMH compliant repository.
 */
@Slf4j
@Service
public class OaiHarvestService {

  private final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();

  /**
   * Harvests a record from an OAI-PMH endpoint based on the given parameters.
   *
   * <p>Fetches metadata and associated identifiers for a specific record,
   * returning a structured representation of the harvested data.
   *
   * @param oaiEndpoint the URL of the OAI-PMH endpoint.
   * @param oaiSet the specific set within the OAI-PMH endpoint to harvest from.
   * @param oaiMetadataPrefix the metadata format to be used for harvesting.
   * @param datasetId the identifier for the dataset associated with the record.
   * @param sourceRecordId the identifier of the source record to be harvested.
   * @return a {@link HarvestedRecord} containing the harvested record details.
   * @throws OaiHarvestException if an error occurs during the harvesting process.
   */
  public HarvestedRecord harvestRecord(String oaiEndpoint, String oaiSet, String oaiMetadataPrefix, String datasetId,
      String sourceRecordId) throws OaiHarvestException {
    log.info("Harvesting record: {}", sourceRecordId);

    OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
    OaiRecord oaiRecord = getOaiRecord(sourceRecordId, oaiHarvest);

    String recordData = new String(oaiRecord.getContent().readAllBytes(), StandardCharsets.UTF_8);

    Optional<EuropeanaGeneratedIdsMap> europeanaGeneratedIdsMap = getEuropeanaGeneratedIdsMap(datasetId, recordData);
    String sourceProvidedChoAbout = europeanaGeneratedIdsMap.map(EuropeanaGeneratedIdsMap::getSourceProvidedChoAbout)
                                                            .orElse(sourceRecordId);
    String recordId = europeanaGeneratedIdsMap.map(EuropeanaGeneratedIdsMap::getEuropeanaGeneratedId).orElse(sourceRecordId);
    return new HarvestedRecord(sourceProvidedChoAbout, recordId, recordData);
  }

  private OaiRecord getOaiRecord(String sourceRecordId, OaiHarvest oaiHarvest) throws OaiHarvestException {
    try {
      return oaiHarvester.harvestRecord(oaiHarvest, sourceRecordId);
    } catch (HarvesterException e) {
      throw new OaiHarvestException(e);
    }
  }

  private Optional<EuropeanaGeneratedIdsMap> getEuropeanaGeneratedIdsMap(String datasetId, String recordData) {
    EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = null;
    try {
      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(recordData, datasetId);
    } catch (EuropeanaIdException e) {
      log.debug("Reading edm ids failed(probably not edm format), proceed without them", e);
    }
    return ofNullable(europeanaGeneratedIdsMap);
  }

  /**
   * Exception class for errors that occur during OAI-PMH harvesting.
   */
  @StandardException
  public static class OaiHarvestException extends Exception {

  }
}

