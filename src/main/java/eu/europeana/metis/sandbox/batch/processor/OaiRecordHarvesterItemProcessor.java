package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createValidated;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParameters;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.workflow.HarvestedRecord;
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

@Component
@StepScope
public class OaiRecordHarvesterItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> {

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

  public OaiRecordHarvesterItemProcessor(HarvestParameterService harvestParameterService,
      OaiHarvestService oaiHarvestService) {
    this.harvestParameterService = harvestParameterService;
    this.oaiHarvestService = oaiHarvestService;
  }

  @PostConstruct
  private void prepare() {
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.getHarvestingParametersById(UUID.fromString(harvestParameterId)).orElseThrow();
    if (harvestParametersEntity instanceof OaiHarvestParameters oaiHarvestParameters) {
      oaiEndpoint = oaiHarvestParameters.getUrl();
      oaiSet = oaiHarvestParameters.getSetSpec();
      oaiMetadataPrefix = oaiHarvestParameters.getMetadataFormat();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for OaiHarvest");
    }
  }

  @Override
  public ExecutionRecordDTO process(ExecutionRecordExternalIdentifier executionRecordExternalIdentifier) throws Exception {
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
  public ThrowingFunction<JobMetadataDTO, ExecutionRecordDTO> getProcessRecordFunction() {
    return null;
  }
}
