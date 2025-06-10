package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.DEBIAS;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
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
public class DebiasJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final BatchJobType BATCH_JOB = DEBIAS;
  public static final String STEP_NAME = "debiasStep";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  public DebiasJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    LOGGER.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(), parallelizeConfig.parallelizeSize());
  }

  @Bean
  public Job debiasBatchJob(JobRepository jobRepository, @Qualifier(STEP_NAME) Step debiasStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(debiasStep)
        .build();
  }

  @Bean(STEP_NAME)
  public Step debiasStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("debiasRepositoryItemReader") RepositoryItemReader<ExecutionRecord> debiasRepositoryItemReader,
      @Qualifier("debiasAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> debiasAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(debiasRepositoryItemReader)
        .processor(debiasAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("debiasRepositoryItemReader")
  @StepScope
  public RepositoryItemReader<ExecutionRecord> debiasRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, parallelizeConfig.chunkSize());
  }

  @Bean("debiasAsyncItemProcessor")
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> debiasAsyncItemProcessor(
      @Qualifier("debiasItemProcessor") ItemProcessor<ExecutionRecord, ExecutionRecordDTO> debiasItemProcessor,
      @Qualifier("debiasStepAsyncTaskExecutor") TaskExecutor debiasStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(debiasItemProcessor);
    asyncItemProcessor.setTaskExecutor(debiasStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  public TaskExecutor debiasStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
