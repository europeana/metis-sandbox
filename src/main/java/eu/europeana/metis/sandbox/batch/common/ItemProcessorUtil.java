package eu.europeana.metis.sandbox.batch.common;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil {

  private final ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> function;

  @NotNull
  public ExecutionRecordDTO processCapturingException(SuccessExecutionRecordDTO successExecutionRecordDTO, String executionId,
      String executionName) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try {
      resultExecutionRecordDTO = function.apply(successExecutionRecordDTO);
    } catch (Exception exception) {
      return FailExecutionRecordDTO.builder()
                                   .datasetId(successExecutionRecordDTO.getDatasetId())
                                   .recordId(successExecutionRecordDTO.getRecordId())
                                   .executionId(executionId)
                                   .executionName(executionName)
                                   .exception(exception).build();
    }
    return resultExecutionRecordDTO;
  }
}
