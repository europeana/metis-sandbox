package eu.europeana.metis.sandbox.batch.common;

import static eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO.createValidated;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil {

  private final ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> function;

  @NotNull
  public ExecutionRecordDTO processCapturingException(JobMetadataDTO jobMetadataDTO) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try {
      resultExecutionRecordDTO = function.apply(jobMetadataDTO);
    } catch (Exception exception) {
      return createValidated(
          b -> b
              .datasetId(jobMetadataDTO.getSuccessExecutionRecordDTO().getDatasetId())
              .recordId(jobMetadataDTO.getSuccessExecutionRecordDTO().getRecordId())
              .executionId(jobMetadataDTO.getTargetExecutionId())
              .executionName(jobMetadataDTO.getTargetExecutionName())
              .exception(exception));
    }
    return resultExecutionRecordDTO;
  }
}
