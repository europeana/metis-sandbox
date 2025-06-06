package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.transformation.service.EuropeanaGeneratedIdsMap;
import eu.europeana.metis.transformation.service.EuropeanaIdCreator;
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

      String resultString = new String(oaiRecord.getContent().readAllBytes(), StandardCharsets.UTF_8);

      EuropeanaIdCreator europeanIdCreator = new EuropeanaIdCreator();
      EuropeanaGeneratedIdsMap europeanaGeneratedIdsMap = europeanIdCreator.constructEuropeanaId(resultString, datasetId);

      return new HarvestedRecord(
          europeanaGeneratedIdsMap.getSourceProvidedChoAbout(),
          europeanaGeneratedIdsMap.getEuropeanaGeneratedId(),
          resultString
      );

    } catch (Exception e) {
      throw new RuntimeException("Failed to harvest record " + sourceRecordId, e);
    }
  }
}

