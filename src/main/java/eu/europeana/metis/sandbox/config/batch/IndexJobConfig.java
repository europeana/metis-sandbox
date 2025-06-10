package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.INDEX;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class IndexJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final BatchJobType BATCH_JOB = INDEX;
  public static final String STEP_NAME = "indexStep";

  @Value("${index.chunkSize:5}")
  public int chunkSize;
  @Value("${index.parallelizationSize:1}")
  public int parallelization;

  @Bean
  public Job indexJob(JobRepository jobRepository,
      @Qualifier(STEP_NAME) Step indexStep) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(indexStep)
        .build();
  }

  @Bean(STEP_NAME)
  public Step indexStep(
      JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("indexRepositoryItemReader") RepositoryItemReader<ExecutionRecord> indexRepositoryItemReader,
      @Qualifier("indexAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> indexAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
        .reader(indexRepositoryItemReader)
        .processor(indexAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("indexRepositoryItemReader")
  @StepScope
  public RepositoryItemReader<ExecutionRecord> indexRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, chunkSize);
  }

  @Bean("indexAsyncItemProcessor")
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> indexAsyncItemProcessor(
      @Qualifier("indexItemProcessor") ItemProcessor<ExecutionRecord, ExecutionRecordDTO> indexItemProcessor,
      @Qualifier("indexStepAsyncTaskExecutor") TaskExecutor indexAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(indexItemProcessor);
    asyncItemProcessor.setTaskExecutor(indexAsyncTaskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  public TaskExecutor indexStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }
}
