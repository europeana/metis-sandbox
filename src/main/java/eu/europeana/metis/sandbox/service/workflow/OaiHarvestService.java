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
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.experimental.StandardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OaiHarvestService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final OaiHarvester oaiHarvester;

  public OaiHarvestService() {
    this.oaiHarvester = HarvesterFactory.createOaiHarvester();
  }

  public HarvestedRecord harvestRecord(String oaiEndpoint, String oaiSet, String oaiMetadataPrefix, String datasetId,
      String sourceRecordId) throws OaiHarvestException {
    LOGGER.info("Harvesting record: {}", sourceRecordId);

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
      LOGGER.debug("Reading edm ids failed(probably not edm format), proceed without them", e);
    }
    return ofNullable(europeanaGeneratedIdsMap);
  }

  @StandardException
  public static class OaiHarvestException extends Exception {

  }
}

