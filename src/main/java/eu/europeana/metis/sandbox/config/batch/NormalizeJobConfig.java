package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.NORMALIZE;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingItemProcessListener;
import eu.europeana.metis.sandbox.batch.reader.DefaultRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
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

@Configuration
public class NormalizeJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final BatchJobType BATCH_JOB = NORMALIZE;
  public static final String STEP_NAME = "normalizeStep";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  public NormalizeJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    LOGGER.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(),
        parallelizeConfig.parallelizeSize());
  }

  @Bean
  public Job normalizeBatchJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step normalizeStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(normalizeStep)
        .build();
  }

  @Bean(STEP_NAME)
  public Step normalizeStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("normalizeRepositoryItemReader") RepositoryItemReader<ExecutionRecord> normalizeRepositoryItemReader,
      @Qualifier("normalizeAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> normalizeAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(normalizeRepositoryItemReader)
        .processor(normalizeAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("normalizeRepositoryItemReader")
  @StepScope
  public RepositoryItemReader<ExecutionRecord> normalizeRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, parallelizeConfig.chunkSize());
  }

  @Bean("normalizeAsyncItemProcessor")
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> normalizeAsyncItemProcessor(
      @Qualifier("normalizeItemProcessor") ItemProcessor<ExecutionRecord, ExecutionRecordDTO> normalizeItemProcessor,
      @Qualifier("normalizeStepAsyncTaskExecutor") TaskExecutor normalizeStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(normalizeItemProcessor);
    asyncItemProcessor.setTaskExecutor(normalizeStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  public TaskExecutor normalizeStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
