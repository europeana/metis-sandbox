package eu.europeana.metis.sandbox.batch.processor;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.common.BatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchBatchJobSubType;
import eu.europeana.metis.sandbox.batch.dto.JobMetadataDTO;
import eu.europeana.metis.sandbox.batch.dto.SuccessExecutionRecordDTO;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.function.ThrowingFunction;

@Getter
public abstract class AbstractMetisItemProcessor<I, O> implements ItemProcessor<I, O> {

  @Value("#{stepExecution.jobExecution.jobInstance.jobName}")
  private String jobName;
  @Value("#{jobParameters['targetExecutionId']}")
  private String targetExecutionId;
  @Value("#{jobParameters['batchJobSubType']}")
  private String batchJobSubTypeString;

  private BatchJobType batchJobType;
  private BatchJobSubType batchJobSubType;

  @PostConstruct
  public void init() {
    this.batchJobType = BatchJobType.valueOf(jobName);
    if (StringUtils.isNotBlank(batchJobSubTypeString)) {
      this.batchJobSubType = ValidationBatchBatchJobSubType.valueOf(batchJobSubTypeString);
    }
  }

  abstract ThrowingFunction<JobMetadataDTO, SuccessExecutionRecordDTO> getProcessRecordFunction();

  String getExecutionName() {
    return batchJobSubType == null
        ? batchJobType.name()
        : format("%s-%s", batchJobType.name(), batchJobSubType.name());
  }

}
