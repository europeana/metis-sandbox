package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
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

  abstract ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction();

  String getExecutionName() {
    return fullBatchJobType.name();
  }
}
