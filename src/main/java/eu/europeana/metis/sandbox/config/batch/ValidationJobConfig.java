package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.VALIDATION;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.TimestampJobParametersIncrementer;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecord;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingChunkListener;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingItemProcessListener;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingJobExecutionListener;
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
public class ValidationJobConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final BatchJobType BATCH_JOB = VALIDATION;
  public static final String STEP_NAME = "validationStep";

  @Value("${validation.chunkSize:5}")
  public int chunkSize;
  @Value("${validation.parallelizationSize:1}")
  public int parallelization;

  @Bean
  public Job validationBatchJob(JobRepository jobRepository,
      @Qualifier("validationStep") Step validationStep,
      LoggingJobExecutionListener loggingJobExecutionListener) {
    LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .incrementer(new TimestampJobParametersIncrementer())
        .listener(loggingJobExecutionListener)
        .start(validationStep)
        .build();
  }

  @Bean("validationStep")
  public Step validationStep(JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("validationRepositoryItemReader") RepositoryItemReader<ExecutionRecord> validationRepositoryItemReader,
      @Qualifier("validationAsyncItemProcessor") ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> validationAsyncItemProcessor,
      ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecord> loggingItemProcessListener,
      LoggingChunkListener loggingChunkListener) {
    return new StepBuilder(STEP_NAME, jobRepository)
        .<ExecutionRecord, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
        .reader(validationRepositoryItemReader)
        .processor(validationAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .listener(loggingChunkListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("validationRepositoryItemReader")
  @StepScope
  public RepositoryItemReader<ExecutionRecord> validationRepositoryItemReader(
      ExecutionRecordRepository executionRecordRepository) {
    return new DefaultRepositoryItemReader(executionRecordRepository, chunkSize);
  }

  @Bean
  public TaskExecutor validationStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(VALIDATION.name() + "-");
    executor.setCorePoolSize(parallelization);
    executor.setMaxPoolSize(parallelization);
    executor.initialize();
    return executor;
  }

  @Bean("validationAsyncItemProcessor")
  public ItemProcessor<ExecutionRecord, Future<ExecutionRecordDTO>> validationAsyncItemProcessor(
      @Qualifier("validationItemProcessor") ItemProcessor<ExecutionRecord, ExecutionRecordDTO> validationItemProcessor,
      @Qualifier("validationStepAsyncTaskExecutor") TaskExecutor validationStepAsyncTaskExecutor) {
    AsyncItemProcessor<ExecutionRecord, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(validationItemProcessor);
    asyncItemProcessor.setTaskExecutor(validationStepAsyncTaskExecutor);
    return asyncItemProcessor;
  }
}
