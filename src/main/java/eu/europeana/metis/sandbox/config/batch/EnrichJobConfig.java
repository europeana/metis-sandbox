package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.ENRICH;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingItemProcessListener;
import eu.europeana.metis.sandbox.batch.reader.DefaultRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configuration class for the Enrich Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class EnrichJobConfig {

  public static final BatchJobType BATCH_JOB = ENRICH;
  public static final String STEP_NAME = "enrichStep";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  EnrichJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    log.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(),
        parallelizeConfig.parallelizeSize());
  }

  @Bean
  Job enrichBatchJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step enrichStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(enrichStep)
        .build();
  }

  @Bean(STEP_NAME)
  Step enrichStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("enrichRepositoryItemReader") RepositoryItemReader<ExecutionRecord> enrichRepositoryItemReader,
      @Qualifier("enrichAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<AbstractExecutionRecordDTO>> enrichAsyncItemProcessor,
      ItemWriter<Future<AbstractExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<AbstractExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(enrichRepositoryItemReader)
        .processor(enrichAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("enrichRepositoryItemReader")
  @StepScope
  RepositoryItemReader<ExecutionRecord> enrichRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, parallelizeConfig.chunkSize());
  }

  @Bean("enrichAsyncItemProcessor")
  ItemProcessor<ExecutionRecord, Future<AbstractExecutionRecordDTO>> enrichAsyncItemProcessor(
      @Qualifier("enrichItemProcessor") ItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> enrichItemProcessor,
      @Qualifier("enrichStepAsyncTaskExecutor") TaskExecutor enrichStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, AbstractExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(enrichItemProcessor);
    asyncItemProcessor.setTaskExecutor(enrichStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  TaskExecutor enrichStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
