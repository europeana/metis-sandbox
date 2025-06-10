package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.TRANSFORM;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransforJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final BatchJobType BATCH_JOB = TRANSFORM;
  public static final String STEP_NAME = "transformStep";

  @Value("${transform.chunkSize:5}")
  public int chunkSize;
  @Value("${transform.parallelizationSize:1}")
  public int parallelization;

  @Bean
  public Job transformBatchJob(JobRepository jobRepository,
      @Qualifier(STEP_NAME) Step tranformStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(tranformStep)
        .build();
  }

  @Bean(STEP_NAME)
  public Step transformStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("transformRepositoryItemReader") RepositoryItemReader<ExecutionRecord> transformRepositoryItemReader,
      @Qualifier("transformAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> transformAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
        .reader(transformRepositoryItemReader)
        .processor(transformAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("transformRepositoryItemReader")
  @StepScope
  public RepositoryItemReader<ExecutionRecord> transformRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, chunkSize);
  }

  @Bean
  public TaskExecutor transformStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }

  @Bean("transformAsyncItemProcessor")
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> transformAsyncItemProcessor(
      @Qualifier("transformItemProcessor") ItemProcessor<ExecutionRecord, ExecutionRecordDTO> transformItemProcessor,
      @Qualifier("transformStepAsyncTaskExecutor") TaskExecutor transformStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(transformItemProcessor);
    asyncItemProcessor.setTaskExecutor(transformStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }
}
