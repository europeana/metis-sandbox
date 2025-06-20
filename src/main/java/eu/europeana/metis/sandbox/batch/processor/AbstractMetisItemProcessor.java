package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import jakarta.annotation.PostConstruct;
import java.util.function.BiFunction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.function.ThrowingFunction;

/**
 * Abstract base class for item processors.
 *
 * <p>Provides foundational functionalities for processing input items into output items, including support for error handling
 * and structured initialization of batch job settings.
 *
 * @param <I> The type of input items to process.
 * @param <O> The type of output items produced by processing.
 */
@Slf4j
@Getter
public abstract class AbstractMetisItemProcessor<I, O> implements ItemProcessor<I, O> {

  @Value("#{stepExecution.jobExecution.jobInstance.jobName}")
  private String jobName;
  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['batchJobSubType']}")
  private String batchJobSubTypeString;

  private FullBatchJobType fullBatchJobType;

  /**
   * Initializes the processor by determining the batch job type based on job name and subtype.
   *
   * <p>Ensures that the `fullBatchJobType` is validated and correctly identified.
   */
  @PostConstruct
  public void init() {
    fullBatchJobType = FullBatchJobType.validateAndGetFullBatchJobType(jobName, batchJobSubTypeString);
    log.info("Initializing batch job type: {}", fullBatchJobType.name());
  }

  abstract ThrowingFunction<JobMetadataDTO, AbstractExecutionRecordDTO> getProcessRecordFunction();

  String getExecutionName() {
    return fullBatchJobType.name();
  }

  /**
   * Processes the given input using the provided function and applies an exception handler in case of errors.
   *
   * @param <T> The type of the input object.
   * @param <R> The type of the result produced by the function.
   * @param input The input object to be processed.
   * @param function The function to apply to the input object, potentially throwing an exception.
   * @param exceptionHandler The handler to execute if an exception occurs during processing, receiving the input and the exception.
   * @return The result produced by either the function or the exception handler.
   */
  public static <T, R> R processCapturingException(
      T input,
      ThrowingFunction<T, R> function,
      BiFunction<T, Exception, R> exceptionHandler) {
    try {
      return function.apply(input);
    } catch (Exception e) {
      log.warn("Exception occurred while processing input {}: {}", input, e.getMessage(), e);
      return exceptionHandler.apply(input, e);
    }
  }
}
