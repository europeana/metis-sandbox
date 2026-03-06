package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.common.batch.BatchJobType.NORMALIZE;

import eu.europeana.metis.sandbox.common.batch.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.reader.DefaultRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration class for the Normalize Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class NormalizeJobConfig {

  public static final BatchJobType BATCH_JOB = NORMALIZE;
  public static final String STEP_NAME = "normalizeStep";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  NormalizeJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    log.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(),
        parallelizeConfig.parallelizeSize());
  }

  @Bean
  Job normalizeBatchJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step normalizeStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(normalizeStep)
        .build();
  }

  @Bean(STEP_NAME)
  Step normalizeStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("normalizeRepositoryItemReader") RepositoryItemReader<ExecutionRecord> normalizeRepositoryItemReader,
      @Qualifier("normalizeAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<AbstractExecutionRecordDTO>> normalizeAsyncItemProcessor,
      ItemWriter<Future<AbstractExecutionRecordDTO>> executionRecordDTOAsyncItemWriter) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<AbstractExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize())
        .transactionManager(transactionManager)
        .reader(normalizeRepositoryItemReader)
        .processor(normalizeAsyncItemProcessor)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("normalizeRepositoryItemReader")
  @StepScope
  RepositoryItemReader<ExecutionRecord> normalizeRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, parallelizeConfig.chunkSize());
  }

  @Bean("normalizeAsyncItemProcessor")
  ItemProcessor<ExecutionRecord, Future<AbstractExecutionRecordDTO>> normalizeAsyncItemProcessor(
      @Qualifier("normalizeItemProcessor") ItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> normalizeItemProcessor,
      @Qualifier("normalizeStepAsyncTaskExecutor") TaskExecutor normalizeStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> asyncItemProcessor
        = new AsyncItemProcessor<>(normalizeItemProcessor);
    asyncItemProcessor.setTaskExecutor(normalizeStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  TaskExecutor normalizeStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
