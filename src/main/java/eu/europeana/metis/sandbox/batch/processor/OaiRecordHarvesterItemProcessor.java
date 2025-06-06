package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createValidated;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.service.workflow.HarvestedRecord;
import eu.europeana.metis.sandbox.service.workflow.OaiHarvestService;
import java.lang.invoke.MethodHandles;
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

  @Value("#{jobParameters['oaiEndpoint']}")
  private String oaiEndpoint;
  @Value("#{jobParameters['oaiSet']}")
  private String oaiSet;
  @Value("#{jobParameters['oaiMetadataPrefix']}")
  private String oaiMetadataPrefix;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;

  private final OaiHarvestService oaiHarvestService;

  public OaiRecordHarvesterItemProcessor(OaiHarvestService oaiHarvestService) {
    this.oaiHarvestService = oaiHarvestService;
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
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return null;
  }
}
