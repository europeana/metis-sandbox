package eu.europeana.metis.sandbox.batch.reader;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifierKey;
import eu.europeana.metis.sandbox.common.FileType;
import eu.europeana.metis.sandbox.entity.harvest.AbstractBinaryHarvestParametersEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.service.dataset.DatasetExecutionSetupService;
import eu.europeana.metis.sandbox.service.dataset.HarvestParameterService;
import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestTarget;
import eu.europeana.metis.sandbox.service.workflow.harvest.HarvestIdentifiersResult;
import eu.europeana.metis.sandbox.service.workflow.harvest.FileHarvestService;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.utils.StringUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@StepScope
@Component
public class FileIdentifiersItemReader implements ItemReader<ExecutionRecordExternalIdentifier> {

  private static final BatchJobType batchJobType = HARVEST_FILE;

  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['harvestParameterId']}")
  private String harvestParameterId;
  @Value("#{jobParameters['datasetId']}")
  private String datasetId;
  @Value("#{jobParameters['stepSize']}")
  private String stepSize;

  private final HarvestParameterService harvestParameterService;
  private final FileHarvestService fileHarvestService;
  private final DatasetExecutionSetupService datasetExecutionSetupService;
  private final List<String> fileIdentifiers = new LinkedList<>();

  public FileIdentifiersItemReader(HarvestParameterService harvestParameterService, FileHarvestService fileHarvestService,
      DatasetExecutionSetupService datasetExecutionSetupService) {
    this.harvestParameterService = harvestParameterService;
    this.fileHarvestService = fileHarvestService;
    this.datasetExecutionSetupService = datasetExecutionSetupService;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    try {
      harvestIdentifiers();
    } catch (RuntimeException exception) {
      datasetExecutionSetupService.updateDatasetWithError(datasetId, exception);
      throw exception;
    }
  }

  @Override
  public ExecutionRecordExternalIdentifier read() {

    final String identifier = takeIdentifier();
    if (StringUtils.isBlank(identifier)) {
      return null;
    } else {
      ExecutionRecordExternalIdentifierKey executionRecordIdentifierKey = new ExecutionRecordExternalIdentifierKey();
      executionRecordIdentifierKey.setDatasetId(datasetId);
      executionRecordIdentifierKey.setExecutionId(targetExecutionId);
      executionRecordIdentifierKey.setExecutionName(batchJobType.name());
      executionRecordIdentifierKey.setSourceRecordId(identifier);

      ExecutionRecordExternalIdentifier recordIdentifier = new ExecutionRecordExternalIdentifier();
      recordIdentifier.setIdentifier(executionRecordIdentifierKey);
      recordIdentifier.setDeleted(false);

      return recordIdentifier;
    }
  }

  private void harvestIdentifiers() {
    HarvestParametersEntity harvestParametersEntity =
        harvestParameterService.getHarvestingParametersById(UUID.fromString(harvestParameterId)).orElseThrow();

    String fileName;
    FileType fileType;
    byte[] fileContent;

    if (harvestParametersEntity instanceof AbstractBinaryHarvestParametersEntity abstractBinaryHarvestParametersEntity) {
      fileName = abstractBinaryHarvestParametersEntity.getFileName();
      fileType = abstractBinaryHarvestParametersEntity.getFileType();
      fileContent = abstractBinaryHarvestParametersEntity.getFileContent();
    } else {
      throw new IllegalArgumentException("Unsupported HarvestParametersEntity type for FileHarvest");
    }

    //We have the parameters, we can harvest the identifiers
    log.info("Harvesting identifiers for {}", fileName);
    FileHarvestTarget fileHarvestTarget = new FileHarvestTarget(fileName, fileType, fileContent);
    HarvestIdentifiersResult<String> harvestIdentifiersResult = fileHarvestService.harvestExternalIdentifiers(
        fileHarvestTarget, Integer.valueOf(stepSize));
    fileIdentifiers.addAll(harvestIdentifiersResult.identifiers());
    if (harvestIdentifiersResult.recordLimitExceeded()) {
      datasetExecutionSetupService.updateRecordLimitExceeded(Integer.parseInt(datasetId));
    }
    log.info("Identifiers harvested");
  }

  private synchronized String takeIdentifier() {
    if (fileIdentifiers.isEmpty()) {
      return null;
    } else {
      return fileIdentifiers.removeFirst();
    }
  }
}
