package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.common.batch.BatchJobType.DEBIAS;

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
 * Configuration class for the Debias Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class DebiasJobConfig {

  public static final BatchJobType BATCH_JOB = DEBIAS;
  public static final String STEP_NAME = "debiasStep";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  DebiasJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    log.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(), parallelizeConfig.parallelizeSize());
  }

  @Bean
  Job debiasBatchJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step debiasStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(debiasStep)
        .build();
  }

  @Bean(STEP_NAME)
  Step debiasStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("debiasRepositoryItemReader") RepositoryItemReader<ExecutionRecord> debiasRepositoryItemReader,
      @Qualifier("debiasAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<AbstractExecutionRecordDTO>> debiasAsyncItemProcessor,
      ItemWriter<Future<AbstractExecutionRecordDTO>> executionRecordDTOAsyncItemWriter) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<AbstractExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize())
        .transactionManager(transactionManager)
        .reader(debiasRepositoryItemReader)
        .processor(debiasAsyncItemProcessor)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("debiasRepositoryItemReader")
  @StepScope
  RepositoryItemReader<ExecutionRecord> debiasRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, parallelizeConfig.chunkSize());
  }

  @Bean("debiasAsyncItemProcessor")
  ItemProcessor<ExecutionRecord, Future<AbstractExecutionRecordDTO>> debiasAsyncItemProcessor(
      @Qualifier("debiasItemProcessor") ItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> debiasItemProcessor,
      @Qualifier("debiasStepAsyncTaskExecutor") TaskExecutor debiasStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> asyncItemProcessor
        = new AsyncItemProcessor<>(debiasItemProcessor);
    asyncItemProcessor.setTaskExecutor(debiasStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  TaskExecutor debiasStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
