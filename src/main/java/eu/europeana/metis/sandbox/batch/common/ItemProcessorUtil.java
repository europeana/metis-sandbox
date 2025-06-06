package eu.europeana.metis.sandbox.batch.common;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.FailExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.function.ThrowingFunction;

@AllArgsConstructor
public class ItemProcessorUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

//  private final ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> function;

//  @NotNull
//  public ExecutionRecordDTO processCapturingException(JobMetadataDTO jobMetadataDTO) {
//    ExecutionRecordDTO resultExecutionRecordDTO;
//    try {
//      resultExecutionRecordDTO = function.apply(jobMetadataDTO);
//    } catch (Exception exception) {
//      return createValidated(
//          b -> b
//              .datasetId(jobMetadataDTO.getSuccessExecutionRecordDTO().getDatasetId())
//              .recordId(jobMetadataDTO.getSuccessExecutionRecordDTO().getRecordId())
//              .executionId(jobMetadataDTO.getTargetExecutionId())
//              .executionName(jobMetadataDTO.getTargetExecutionName())
//              .exception(exception));
//    }
//    return resultExecutionRecordDTO;
//  }

  public static <T, R> R processCapturingException(
      T input, ThrowingFunction<T, R> function, BiFunction<T, Exception, R> exceptionHandler
  ) {
    try {
      return function.apply(input);
    } catch (Exception e) {
      LOGGER.warn(format("Exception occurred while processing input %s: %s", input, e.getMessage()), e);
      return exceptionHandler.apply(input, e);
    }
  }


  /**
   * Default exception handler for JobMetadataDTO → ExecutionRecordDTO.
   */
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
