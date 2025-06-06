package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.common.ExecutionRecordAndDTOConverterUtil;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractExecutionRecordMetisItemProcessor extends
    AbstractMetisItemProcessor<ExecutionRecord, ExecutionRecordDTO> {

  @Override
  public ExecutionRecordDTO process(@NotNull ExecutionRecord executionRecord) {
    SuccessExecutionRecordDTO originSuccessExecutionRecordDTO =
        ExecutionRecordAndDTOConverterUtil.converterToExecutionRecordDTO(executionRecord);

    JobMetadataDTO jobMetadataDTO =
        new JobMetadataDTO(originSuccessExecutionRecordDTO, getExecutionName(), getTargetExecutionId());

    return processCapturingException(jobMetadataDTO, getProcessRecordFunction(), defaultHandler()
    );
  }

  public static BiFunction<JobMetadataDTO, Exception, ExecutionRecordDTO> defaultHandler() {
    return (jobMetadataDTO, exception) -> FailExecutionRecordDTO.createValidated(
        b -> b
            .datasetId(jobMetadataDTO.getSuccessExecutionRecordDTO().getDatasetId())
            .recordId(jobMetadataDTO.getSuccessExecutionRecordDTO().getRecordId())
            .executionId(jobMetadataDTO.getTargetExecutionId())
            .executionName(jobMetadataDTO.getTargetExecutionName())
            .exception(exception)
    );
  }

}
