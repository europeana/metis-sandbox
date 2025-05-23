package eu.europeana.metis.sandbox.batch.common;

import static eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO.createValidated;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil {

  private final ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> function;

  @NotNull
  public ExecutionRecordDTO processCapturingException(SuccessExecutionRecordDTO originSuccessExecutionRecordDTO,
      String executionId,
      String executionName) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try {
      resultExecutionRecordDTO = function.apply(originSuccessExecutionRecordDTO);
    } catch (Exception exception) {
      return createValidated(
          b -> b
              .datasetId(originSuccessExecutionRecordDTO.getDatasetId())
              .recordId(originSuccessExecutionRecordDTO.getRecordId())
              .executionId(executionId)
              .executionName(executionName)
              .exception(exception));
    }
    return resultExecutionRecordDTO;
  }
}
