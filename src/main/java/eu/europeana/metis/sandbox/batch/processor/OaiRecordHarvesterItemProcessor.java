package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.service.workflow.OaiHarvestService;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for oai record harvest.
 */
@Component
@StepScope
public class OaiRecordHarvesterItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{jobParameters['harvestParameterId']}")
  private String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  private String oaiEndpoint;
  private String oaiSet;
  private String oaiMetadataPrefix;

  private final HarvestParameterService harvestParameterService;
  private final OaiHarvestService oaiHarvestService;

  /**
   * Constructor with service parameters.
   *
   * @param harvestParameterService The service responsible for providing the harvesting parameters.
   * @param oaiHarvestService The service responsible for oai harvesting record data.
   */
  public OaiRecordHarvesterItemProcessor(HarvestParameterService harvestParameterService,
      OaiHarvestService oaiHarvestService) {
    this.harvestParameterService = harvestParameterService;
    this.oaiHarvestService = oaiHarvestService;
  }

  /**
   * Prepares the OAI harvesting configuration by retrieving and validating the necessary parameters.
   *
   * <p>Fetches the harvesting parameters from the {@link HarvestParameterService}.
   * <p>Extracts OAI-specific configuration details such as endpoint, set specification,
   * and metadata prefix if the parameters are of type {@link OaiHarvestParametersEntity}.
   * <p>Throws an {@link IllegalArgumentException} if the retrieved parameters are not of the expected type.
   */
  @PostConstruct
  private void prepare() {
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.getHarvestingParametersById(UUID.fromString(harvestParameterId)).orElseThrow();
    if (harvestParametersEntity instanceof OaiHarvestParametersEntity oaiHarvestParametersEntity) {
      oaiEndpoint = oaiHarvestParametersEntity.getUrl();
      oaiSet = oaiHarvestParametersEntity.getSetSpec();
      oaiMetadataPrefix = oaiHarvestParametersEntity.getMetadataFormat();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for OaiHarvest");
    }
  }

  @Override
  public AbstractExecutionRecordDTO process(ExecutionRecordExternalIdentifier executionRecordExternalIdentifier) throws Exception {
    LOGGER.info("OaiHarvestItemReader thread: {}", Thread.currentThread());

    HarvestedRecord harvestedRecord = oaiHarvestService.harvestRecord(
        oaiEndpoint,
        oaiSet,
        oaiMetadataPrefix,
        datasetId,
        executionRecordExternalIdentifier.getIdentifier().getSourceRecordId()
    );

    return createValidated(b -> b
        .datasetId(datasetId)
        .executionId(getTargetExecutionId())
        .sourceRecordId(harvestedRecord.sourceRecordId())
        .recordId(harvestedRecord.recordId())
        .executionName(getExecutionName())
        .recordData(harvestedRecord.recordData()));
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return null;
  }
}
