package eu.europeana.metis.sandbox.batch.processor;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.function.BiFunction;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.function.ThrowingFunction;

@Getter
public abstract class AbstractMetisItemProcessor<I, O> implements ItemProcessor<I, O> {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("#{stepExecution.jobExecution.jobInstance.jobName}")
  private String jobName;
  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['batchJobSubType']}")
  private String batchJobSubTypeString;

  private FullBatchJobType fullBatchJobType;

  @PostConstruct
  public void init() {
    fullBatchJobType = FullBatchJobType.validateAndGetFullBatchJobType(jobName, batchJobSubTypeString);
    LOGGER.info("Initializing batch job type: {}", fullBatchJobType.name());
  }

  abstract ThrowingFunction<JobMetadataDTO, ExecutionRecordDTO> getProcessRecordFunction();

  String getExecutionName() {
    return fullBatchJobType.name();
  }

  public static <T, R> R processCapturingException(
      T input,
      ThrowingFunction<T, R> function,
      BiFunction<T, Exception, R> exceptionHandler) {
    try {
      return function.apply(input);
    } catch (Exception e) {
      LOGGER.warn(format("Exception occurred while processing input %s: %s", input, e.getMessage()), e);
      return exceptionHandler.apply(input, e);
    }
  }
}
