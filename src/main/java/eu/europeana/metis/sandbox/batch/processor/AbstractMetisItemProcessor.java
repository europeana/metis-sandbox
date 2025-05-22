package eu.europeana.metis.sandbox.batch.processor;

import eu.europeana.metis.sandbox.batch.common.BatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.function.ThrowingFunction;

public abstract class AbstractMetisItemProcessor<I, O> implements ItemProcessor<I, O> {

  @Value("#{stepExecution.jobExecution.jobInstance.jobName}")
  private String jobName;

  private BatchJobType batchJobType;

  @PostConstruct
  public void init() {
    this.batchJobType = BatchJobType.valueOf(jobName);
  }

  abstract ThrowingFunction<SuccessExecutionRecordDTO, SuccessExecutionRecordDTO> getProcessRecordFunction();

  String getExecutionName(BatchJobSubType batchJobSubType) {
    return getExecutionName() + "-" + batchJobSubType.getName();
  }

  String getExecutionName() {
    return batchJobType.name();
  }

}
