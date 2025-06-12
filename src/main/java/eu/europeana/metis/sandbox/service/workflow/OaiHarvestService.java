package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
import eu.europeana.metis.transformation.service.EuropeanaIdException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
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

  public HarvestedRecord harvestRecord(String oaiEndpoint, String oaiSet, String oaiMetadataPrefix, String datasetId, String sourceRecordId) {
    try {
      LOGGER.info("Harvesting record: {}", sourceRecordId);

      OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
      OaiRecord oaiRecord = oaiHarvester.harvestRecord(oaiHarvest, sourceRecordId);

      String recordData = new String(oaiRecord.getContent().readAllBytes(), StandardCharsets.UTF_8);

      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      String sourceProvidedChoAbout = sourceRecordId;
      String recordId = sourceRecordId;
      try {
        EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(recordData, datasetId);
        sourceProvidedChoAbout = europeanaGeneratedIdsMap.getSourceProvidedChoAbout();
        recordId = europeanaGeneratedIdsMap.getEuropeanaGeneratedId();
      } catch (EuropeanaIdException e) {
        LOGGER.debug("Reading edm ids failed(probably not edm format), proceed without them", e);
      }
      return new HarvestedRecord(sourceProvidedChoAbout, recordId, recordData);

    } catch (Exception e) {
      throw new RuntimeException("Failed to harvest record " + sourceRecordId, e);
    }
  }
}

