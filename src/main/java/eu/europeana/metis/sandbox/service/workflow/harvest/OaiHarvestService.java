package eu.europeana.metis.sandbox.service.workflow.harvest;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.HarvestingIterator;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.common.exception.HarvestException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for harvesting records from an OAI-PMH compliant repository.
 */
@Slf4j
@Service
public class OaiHarvestService implements HarvestService<OaiRecordHeader, OaiHarvest> {

  private static final int STOP_WATCH_INTERNAL_SECONDS = 10;
  private final OaiHarvester oaiHarvester = HarvesterFactory.createOaiHarvester();
  private final int maxAllowedRecords;

  /**
   * Constructor.
   *
   * @param maxAllowedRecords the maximum number of records allowed to be processed
   */
  @Autowired
  public OaiHarvestService(@Value("${sandbox.dataset.max-size}") int maxAllowedRecords) {
    this.maxAllowedRecords = maxAllowedRecords;
  }

  @Override
  public HarvestIdentifiersResult<OaiRecordHeader> harvestExternalIdentifiers(String datasetId, @NotNull OaiHarvest oaiHarvest, Integer stepSize) {
    try (HarvestingIterator<OaiRecordHeader, OaiRecordHeader> recordHeaderIterator =
        oaiHarvester.harvestRecordHeaders(oaiHarvest)) {
      return harvestOaiHeaders(recordHeaderIterator, stepSize);
    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records", e);
    }
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

  @Override
  public int getMaxAllowedRecords() {
    return maxAllowedRecords;
  }

  private OaiRecord getOaiRecord(String sourceRecordId, OaiHarvest oaiHarvest) throws HarvestException {
    try {
      return oaiHarvester.harvestRecord(oaiHarvest, sourceRecordId);
    } catch (HarvesterException e) {
      throw new HarvestException(e);
    }
  }

  private HarvestIdentifiersResult<OaiRecordHeader> harvestOaiHeaders(HarvestingIterator<OaiRecordHeader,
      OaiRecordHeader> iteratorToFilter, Integer stepSize) throws HarvesterException {
    StopWatch watch = StopWatch.createStarted();
    final List<OaiRecordHeader> result = new ArrayList<>();
    HarvestFromIteratorResult harvestFromIteratorResult = harvestFromIterator(iteratorToFilter, stepSize, entry -> {
      result.add(entry);

      if (watch.getTime(TimeUnit.SECONDS) > STOP_WATCH_INTERNAL_SECONDS) {
        log.info("Already harvested {} records...", result.size());
        watch.reset();
        watch.start();
      }
      return IterationResult.CONTINUE;
    }, OaiRecordHeader::isDeleted);
    return new HarvestIdentifiersResult<>(result, harvestFromIteratorResult.recordLimitExceeded());
  }

  /**
   * Represents the result of a harvesting process from an iterator.
   * <p>
   * The class contains information about whether the record limit was exceeded during the harvesting operation.
   *
   * @param recordLimitExceeded a flag indicating if the record limit was exceeded during harvesting.
   */
  public record HarvestFromIteratorResult(boolean recordLimitExceeded) {

  }
}

