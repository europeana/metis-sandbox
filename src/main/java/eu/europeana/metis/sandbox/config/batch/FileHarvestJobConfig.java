package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_FILE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.reader.ExternalIdentifiersRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.reader.FileIdentifiersItemReader;
import eu.europeana.metis.sandbox.batch.writer.ExternalIdentifiersItemWriter;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration class for the File Harvest Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class FileHarvestJobConfig {

  public static final BatchJobType BATCH_JOB = HARVEST_FILE;
  public static final String IDENTIFIERS_HARVEST_STEP_NAME = "fileIdentifiersHarvest";
  public static final String RECORDS_HARVEST_STEP_NAME = "fileRecordsHarvest";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  FileHarvestJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    log.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(),
        parallelizeConfig.parallelizeSize());
  }

  @Bean
  Job fileHarvestJob(
      JobRepository jobRepository,
      @Qualifier(IDENTIFIERS_HARVEST_STEP_NAME) Step identifiersHarvestStep,
      @Qualifier(RECORDS_HARVEST_STEP_NAME) Step recordsHarvestStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(identifiersHarvestStep)
        .next(recordsHarvestStep)
        .build();
  }

  @Bean(IDENTIFIERS_HARVEST_STEP_NAME)
  Step fileIdentifiersHarvestStep(
      FileIdentifiersItemReader fileIdentifiersItemReader,
      ExternalIdentifiersItemWriter externalIdentifiersItemWriter,
      JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {

    return new StepBuilder(IDENTIFIERS_HARVEST_STEP_NAME, jobRepository)
        .<ExecutionRecordExternalIdentifier, ExecutionRecordExternalIdentifier>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(fileIdentifiersItemReader)
        .writer(externalIdentifiersItemWriter)
        .build();
  }

  @Bean(RECORDS_HARVEST_STEP_NAME)
  Step fileRecordsHarvestStep(
      JobRepository jobRepository,
      ExternalIdentifiersRepositoryItemReader externalIdentifiersRepositoryItemReader,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("fileRecordAsyncItemProcessor")
      ItemProcessor<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>> fileRecordAsyncItemProcessor,
      ItemWriter<Future<AbstractExecutionRecordDTO>> executionRecordDTOAsyncItemWriter) {
    return new StepBuilder(RECORDS_HARVEST_STEP_NAME, jobRepository)
        .<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize(),
            transactionManager)
        .reader(externalIdentifiersRepositoryItemReader)
        .processor(fileRecordAsyncItemProcessor)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("fileRecordAsyncItemProcessor")
  ItemProcessor<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>> fileRecordAsyncItemProcessor(
      @Qualifier("fileRecordHarvestItemProcessor") ItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> fileRecordHarvestItemProcessor,
      @Qualifier("fileHarvestStepAsyncTaskExecutor") TaskExecutor taskExecutor) {
    AsyncItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(fileRecordHarvestItemProcessor);
    asyncItemProcessor.setTaskExecutor(taskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  TaskExecutor fileHarvestStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
