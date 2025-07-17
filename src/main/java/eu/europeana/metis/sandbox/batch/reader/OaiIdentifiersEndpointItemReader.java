package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_OAI;

import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifierKey;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.util.HarvestService;
import jakarta.annotation.PostConstruct;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ItemReader implementation for reading OAI identifiers from a specified endpoint.
 *
 * <p>This class reads OAI identifiers for dataset records and prepares them for further processing in a batch job.
 * It leverages the HarvestParameterService for the requested harvest and HarvestServiceImpl to fetch oai identifiers.
 * <p>Identifier per harvest information is stored in the database for further reading and processing.
 *
 * <p>Configurations like target execution ID, harvest parameter ID, dataset ID, and step size are provided via job parameters.
 * The harvested identifiers are managed in a thread-safe manner to ensure proper access during batch execution.
 *
 * <p>Note: This reader is not restartable and is not optimized at its current state.
 */
@Slf4j
@StepScope
@Component
public class OaiIdentifiersEndpointItemReader implements ItemReader<ExecutionRecordExternalIdentifier> {

  private static final BatchJobType batchJobType = HARVEST_OAI;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['harvestParameterId']}")
  private String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['stepSize']}")
  private String stepSize;

  //todo: Align with the FileItemReader. Here we harvestParameterService but in FileItemReader is in FileHarvestService
  private final HarvestParameterService harvestParameterService;
  private final HarvestService harvestService;
  private final List<OaiRecordHeader> oaiRecordHeaders = new LinkedList<>();

  /**
   * Constructor with service parameters.
   *
   * @param harvestParameterService The service used to access and manage harvest parameters.
   * @param harvestService The service used to perform harvesting operations.
   */
  public OaiIdentifiersEndpointItemReader(HarvestParameterService harvestParameterService,
      HarvestService harvestService) {
    this.harvestParameterService = harvestParameterService;
    this.harvestService = harvestService;
  }

  @PostConstruct
  private void prepare() {
    harvestIdentifiers();
  }

  @Override
  public ExecutionRecordExternalIdentifier read() {

    final OaiRecordHeader oaiRecordHeader = takeIdentifier();
    if (oaiRecordHeader == null) {
      return null;
    } else {
      ExecutionRecordExternalIdentifierKey executionRecordIdentifierKey = new ExecutionRecordExternalIdentifierKey();
      executionRecordIdentifierKey.setDatasetId(datasetId);
      executionRecordIdentifierKey.setExecutionId(targetExecutionId);
      executionRecordIdentifierKey.setExecutionName(batchJobType.name());
      executionRecordIdentifierKey.setSourceRecordId(oaiRecordHeader.getOaiIdentifier());

      ExecutionRecordExternalIdentifier recordIdentifier = new ExecutionRecordExternalIdentifier();
      recordIdentifier.setIdentifier(executionRecordIdentifierKey);
      recordIdentifier.setDeleted(oaiRecordHeader.isDeleted());

      return recordIdentifier;
    }
  }

  private void harvestIdentifiers() {
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.getHarvestingParametersById(UUID.fromString(harvestParameterId)).orElseThrow();
    String oaiEndpoint;
    String oaiSet;
    String oaiMetadataPrefix;

    if (harvestParametersEntity instanceof OaiHarvestParametersEntity oaiHarvestParametersEntity) {
      oaiEndpoint = oaiHarvestParametersEntity.getUrl();
      oaiSet = oaiHarvestParametersEntity.getSetSpec();
      oaiMetadataPrefix = oaiHarvestParametersEntity.getMetadataFormat();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for OaiHarvest");
    }

    log.info("Harvesting identifiers for {}", oaiEndpoint);
    OaiHarvest oaiHarvest = new OaiHarvest(oaiEndpoint, oaiMetadataPrefix, oaiSet);
    oaiRecordHeaders.addAll(harvestService.harvestOaiIdentifiers(oaiHarvest, Integer.valueOf(stepSize)));
    log.info("Identifiers harvested");
  }

  private synchronized OaiRecordHeader takeIdentifier() {
    if (oaiRecordHeaders.isEmpty()) {
      return null;
    } else {
      return oaiRecordHeaders.removeFirst();
    }
  }
}
