package eu.europeana.metis.sandbox.batch.processor;

import static eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO.createCopyIdentifiersValidated;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.common.ItemProcessorUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.service.workflow.NormalizeService;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.util.function.ThrowingFunction;

@StepScope
@Component("normalizationItemProcessor")
public class NormalizationItemProcessor extends AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  private final NormalizeService normalizeService;
  private final ItemProcessorUtil itemProcessorUtil;

  public NormalizationItemProcessor(NormalizeService normalizeService) {
    this.normalizeService = normalizeService;
    itemProcessorUtil = new ItemProcessorUtil(getProcessRecordFunction());
  }

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    final SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(
        executionRecord);
    JobMetadataDTO jobMetadataDTO = new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(), getTargetExecutionId());
    return itemProcessorUtil.processCapturingException(jobMetadataDTO);
  }

  @Override
  public ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction() {
    return jobMetadataDTO -> {
      SuccessExecutionRecordDTO originSuccessExecutionRecordDTO = jobMetadataDTO.getSuccessExecutionRecordDTO();
      String result = normalizeService.normalizeRecord(originSuccessExecutionRecordDTO.getRecordData());

      return createCopyIdentifiersValidated(
          originSuccessExecutionRecordDTO,
          jobMetadataDTO.getTargetExecutionId(),
          jobMetadataDTO.getTargetExecutionName(),
          b -> b.recordData(result));
    };
  }
}
