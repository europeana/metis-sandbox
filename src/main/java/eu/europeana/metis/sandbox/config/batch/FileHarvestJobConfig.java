package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.reader.FileItemReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration class for the File Harvest Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class FileHarvestJobConfig {

  public static final String STEP_NAME = "fileHarvestStep";
  public static final BatchJobType BATCH_JOB = HARVEST_FILE;
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  FileHarvestJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    log.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(),
        parallelizeConfig.parallelizeSize());
  }

  //This can alternatively be divided similarly to oai. First read all file names and store them in db and then as a second step process those files(ids)
  @Bean
  Job fileHarvestJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step fileHarvestStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(fileHarvestStep)
        .build();
  }

  @Bean(STEP_NAME)
  Step fileHarvestStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      FileItemReader fileItemReader,
      ItemWriter<AbstractExecutionRecordDTO> executionRecordWriter) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<AbstractExecutionRecordDTO, AbstractExecutionRecordDTO>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(fileItemReader)
        .writer(executionRecordWriter)
        .build();
  }
}
