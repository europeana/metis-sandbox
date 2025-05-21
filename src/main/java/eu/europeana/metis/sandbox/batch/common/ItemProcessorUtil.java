package eu.europeana.metis.sandbox.batch.common;

import static eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil.createFailureExecutionRecordDTO;
import static eu.europeana.metis.sandbox.batch.common.ExecutionRecordUtil.createSuccessExecutionRecordDTO;

import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil<O> {

  private final ThrowingFunction<ExecutionRecordDTO, O> function;
  private final Function<O, String> getRecordString;

  public ExecutionRecordDTO processCapturingException(ExecutionRecordDTO executionRecordDTO, BatchJobType batchJobType,
      BatchJobSubType batchJobSubType, String executionId) {
    final String executionName = batchJobType.name() + "-" + batchJobSubType.getName();
    return getExecutionRecordDTO(executionRecordDTO, executionId, executionName);
  }

  public ExecutionRecordDTO processCapturingException(ExecutionRecordDTO executionRecordDTO, BatchJobType batchJobType, String executionId) {
    final String executionName = batchJobType.name();
    return getExecutionRecordDTO(executionRecordDTO, executionId, executionName);
  }

  @NotNull
  public ExecutionRecordDTO getExecutionRecordDTO(ExecutionRecordDTO executionRecordDTO, String executionId,
      String executionName) {
    ExecutionRecordDTO resultExecutionRecordDTO;
    try {
      final O result = function.apply(executionRecordDTO);
      resultExecutionRecordDTO =
          createSuccessExecutionRecordDTO(executionRecordDTO, getRecordString.apply(result), executionName, executionId);
    } catch (Exception exception) {
      resultExecutionRecordDTO =
          createFailureExecutionRecordDTO(executionRecordDTO, executionName, executionId, exception.getMessage(), formatException(exception));
    }
    return resultExecutionRecordDTO;
  }

  public static String formatException(Throwable throwable) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    return stringWriter.toString();
  }

}
