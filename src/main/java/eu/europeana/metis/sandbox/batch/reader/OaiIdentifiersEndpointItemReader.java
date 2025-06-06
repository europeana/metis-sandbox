package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_OAI;

import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifierKey;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParameters;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.util.HarvestServiceImpl;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class OaiIdentifiersEndpointItemReader implements ItemReader<ExecutionRecordExternalIdentifier> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final BatchJobType batchJobType = HARVEST_OAI;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['harvestParameterId']}")
  private String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['stepSize']}")
  private String stepSize;

  private final HarvestParameterService harvestParameterService;
  private final HarvestServiceImpl harvestServiceImpl;
  private final List<OaiRecordHeader> oaiRecordHeaders = new LinkedList<>();

  public OaiIdentifiersEndpointItemReader(HarvestParameterService harvestParameterService,
      HarvestServiceImpl harvestServiceImpl) {
    this.harvestParameterService = harvestParameterService;
    this.harvestServiceImpl = harvestServiceImpl;
  }

  @PostConstruct
  private void prepare() {
    harvestIdentifiers();
  }

  @Override
  public ExecutionRecordExternalIdentifier read() {

    final OaiRecordHeader oaiRecordHeader = takeIdentifier();
    if (oaiRecordHeader != null) {
      ExecutionRecordExternalIdentifierKey executionRecordIdentifierKey = new ExecutionRecordExternalIdentifierKey();
      executionRecordIdentifierKey.setDatasetId(datasetId);
      executionRecordIdentifierKey.setExecutionId(targetExecutionId);
      executionRecordIdentifierKey.setExecutionName(batchJobType.name());
      executionRecordIdentifierKey.setSourceRecordId(oaiRecordHeader.getOaiIdentifier());

      ExecutionRecordExternalIdentifier recordIdentifier = new ExecutionRecordExternalIdentifier();
      recordIdentifier.setIdentifier(executionRecordIdentifierKey);
      recordIdentifier.setDeleted(oaiRecordHeader.isDeleted());

      return recordIdentifier;
    } else {
      return null;
    }
  }

  private void harvestIdentifiers() {
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.getHarvestingParametersById(UUID.fromString(harvestParameterId)).orElseThrow();
    String oaiEndpoint;
    String oaiSet;
    String oaiMetadataPrefix;

    if (harvestParametersEntity instanceof OaiHarvestParameters oaiHarvestParameters) {
      oaiEndpoint = oaiHarvestParameters.getUrl();
      oaiSet = oaiHarvestParameters.getSetSpec();
      oaiMetadataPrefix = oaiHarvestParameters.getMetadataFormat();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for OaiHarvest");
    }

    LOGGER.info("Harvesting identifiers for {}", oaiEndpoint);
    OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
    oaiRecordHeaders.addAll(harvestServiceImpl.harvestOaiIdentifiers(datasetId, oaiHarvest, Integer.valueOf(stepSize)));
    LOGGER.info("Identifiers harvested");
  }

  private synchronized OaiRecordHeader takeIdentifier() {
    if (!oaiRecordHeaders.isEmpty()) {
      return oaiRecordHeaders.removeFirst();
    } else {
      return null;
    }
  }
}
