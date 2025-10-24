package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.HarvestException;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for harvesting records from an OAI-PMH compliant repository.
 */
@Slf4j
@Service
public class OaiHarvestService implements HarvestService<OaiRecordHeader, OaiHarvest> {

  private final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();

  @Override
  public Iterable<OaiRecordHeader> getIterableHarvestingIdentifiers(String datasetId, @NotNull OaiHarvest oaiHarvest) {
    return oaiHarvester.harvestRecordHeaders(oaiHarvest);
  }

  /**
   * Harvests a record from an OAI-PMH endpoint based on the given parameters.
   *
   * <p>Fetches metadata and associated identifiers for a specific record,
   * returning a structured representation of the harvested data.
   *
   * @param sourceRecordId the identifier of the source record to be harvested.
   * @return a {@link HarvestedRecord} containing the harvested record details.
   * @throws HarvestException if an error occurs during the harvesting process.
   */
  @Override
  public HarvestedRecord harvestRecord(String datasetId, @NotNull OaiHarvest oaiHarvest, String sourceRecordId) throws HarvestException {
    log.info("Harvesting record: {}", sourceRecordId);
    OaiRecord oaiRecord = getOaiRecord(sourceRecordId, oaiHarvest);
    String recordData = new String(oaiRecord.getContent().readAllBytes(), StandardCharsets.UTF_8);
    return new HarvestedRecord(sourceRecordId, recordData);
  }

  private OaiRecord getOaiRecord(String sourceRecordId, OaiHarvest oaiHarvest) throws HarvestException {
    try {
      return oaiHarvester.harvestRecord(oaiHarvest, sourceRecordId);
    } catch (HarvesterException e) {
      throw new HarvestException(e);
    }
  }
}

