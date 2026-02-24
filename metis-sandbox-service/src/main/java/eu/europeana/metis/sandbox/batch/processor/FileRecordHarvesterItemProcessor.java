package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createValidated;

import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.common.HarvestedRecord;
import eu.europeana.metis.sandbox.entity.harvest.AbstractBinaryHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.OaiHarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestService;
import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestTarget;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

/**
 * Processor responsible for oai record harvest.
 */
@Slf4j
@Component("fileRecordHarvestItemProcessor")
@StepScope
public class FileRecordHarvesterItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> {

  @Value("#{jobParameters['harvestParameterId']}")
  private String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  private String fileName;
  private FileType fileType;
  private byte[] fileContent;

  private final HarvestParameterService harvestParameterService;
  private final FileHarvestService fileHarvestService;

  /**
   * Constructor with service parameters.
   *
   * @param harvestParameterService The service responsible for providing the harvesting parameters.
   * @param fileHarvestService The service responsible for file harvesting record data.
   */
  public FileRecordHarvesterItemProcessor(HarvestParameterService harvestParameterService,
      FileHarvestService fileHarvestService) {
    this.harvestParameterService = harvestParameterService;
    this.fileHarvestService = fileHarvestService;
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
    if (harvestParametersEntity instanceof AbstractBinaryHarvestParametersEntity abstractBinaryHarvestParametersEntity) {
      fileName = abstractBinaryHarvestParametersEntity.getFileName();
      fileType = abstractBinaryHarvestParametersEntity.getFileType();
      fileContent = abstractBinaryHarvestParametersEntity.getFileContent();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for FileHarvest");
    }
  }

  @Override
  public AbstractExecutionRecordDTO process(ExecutionRecordExternalIdentifier executionRecordExternalIdentifier)
      throws Exception {
    log.info("FileRecordHarvestItemReader thread: {}", Thread.currentThread());

    FileHarvestTarget fileHarvestTarget = new FileHarvestTarget(fileName, fileType, fileContent);
    HarvestedRecord harvestedRecord = fileHarvestService.harvestRecord(datasetId, fileHarvestTarget,
        executionRecordExternalIdentifier.getDerivedRecordId());
    String externalRecordId = executionRecordExternalIdentifier.getExternalRecordId();

    return createValidated(successExecutionRecordDTOBuilder -> successExecutionRecordDTOBuilder
        .datasetId(datasetId)
        .executionId(getTargetExecutionId())
        .externalRecordId(externalRecordId)
        .sourceRecordId(externalRecordId)
        .recordId(externalRecordId)
        .executionName(getExecutionName())
        .recordData(harvestedRecord.recordData()));
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction() {
    return null;
  }
}

