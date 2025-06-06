package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.service.workflow.ValidationService;
import eu.europeana.validation.model.ValidationResult;
import java.lang.invoke.MethodHandles;
import lombok.experimental.StandardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("validationItemProcessor")
public class ValidationItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ValidationService validationService;
  private final ItemProcessorUtil itemProcessorUtil;

  public ValidationItemProcessor(ValidationService validationService) {
    this.validationService = validationService;
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @Override
  public ExecutionRecordDTO process(@NonNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(),
        getTargetExecutionId());
    return itemProcessorUtil.processCapturingException(jobMetadataDTO);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      ValidationResult result = validationService.validateRecord(
          originSuccessExecutionRecordDTO.getRecordData(),
          originSuccessExecutionRecordDTO.getDatasetId(),
          getExecutionName(),
          (ValidationBatchJobSubType) getFullBatchJobType().getBatchJobSubType()
      );

      if (result.isSuccess()) {
        LOGGER.debug("Validation Success for datasetId {}, recordId {}", originSuccessExecutionRecordDTO.getDatasetId(),
            originSuccessExecutionRecordDTO.getRecordId());
      } else {
        LOGGER.info("Validation Failure for datasetId {}, recordId {}", originSuccessExecutionRecordDTO.getDatasetId(),
            originSuccessExecutionRecordDTO.getRecordId());
        throw new ValidationFailureException(result.getMessage());
      }

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(originSuccessExecutionRecordDTO.getRecordData()));
    };
  }

  @StandardException
  private static class ValidationFailureException extends Exception {

  }
}
