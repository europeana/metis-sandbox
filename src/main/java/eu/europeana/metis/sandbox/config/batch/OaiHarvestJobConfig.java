package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_OAI;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingItemProcessListener;
import eu.europeana.metis.sandbox.batch.reader.OaiIdentifiersEndpointItemReader;
import eu.europeana.metis.sandbox.batch.reader.OaiIdentifiersRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.writer.OaiIdentifiersWriter;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Configuration class for the Oai Harvest Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class OaiHarvestJobConfig {

  public static final BatchJobType BATCH_JOB = HARVEST_OAI;
  public static final String IDENTIFIERS_HARVEST_STEP_NAME = "identifiersHarvest";
  public static final String RECORDS_HARVEST_STEP_NAME = "recordsHarvest";
  private final WorkflowConfigurationProperties.ParallelizeConfig parallelizeConfig;

  OaiHarvestJobConfig(WorkflowConfigurationProperties workflowConfigurationProperties) {
    parallelizeConfig = workflowConfigurationProperties.workflow().get(BATCH_JOB);
    log.info("Chunk size: {}, Parallelization size: {}", parallelizeConfig.chunkSize(),
        parallelizeConfig.parallelizeSize());
  }

  @Bean
  Job oaiHarvestJob(
      JobRepository jobRepository,
      @Qualifier(IDENTIFIERS_HARVEST_STEP_NAME) Step identifiersHarvestStep,
      @Qualifier(RECORDS_HARVEST_STEP_NAME) Step recordsHarvestStep) {
    return new JobBuilder(BATCH_JOB.name(), jobRepository)
        .start(identifiersHarvestStep)
        .next(recordsHarvestStep)
        .build();
  }

  @Bean(IDENTIFIERS_HARVEST_STEP_NAME)
  Step oaidentifiersEndpointHarvestStep(
      OaiIdentifiersEndpointItemReader oaiIdentifiersEndpointItemReader,
      OaiIdentifiersWriter oaiIdentifiersWriter,
      JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor oaiHarvestStepAsyncTaskExecutor) {

    return new StepBuilder(IDENTIFIERS_HARVEST_STEP_NAME, jobRepository)
        .<ExecutionRecordExternalIdentifier, ExecutionRecordExternalIdentifier>chunk(parallelizeConfig.chunkSize(),
            transactionManager)
        .reader(oaiIdentifiersEndpointItemReader)
        .writer(oaiIdentifiersWriter)
        //                .taskExecutor(oaiHarvestStepAsyncTaskExecutor)
        .build();
  }

  @Bean(RECORDS_HARVEST_STEP_NAME)
  Step oaiRecordsHarvestStep(
      JobRepository jobRepository,
      OaiIdentifiersRepositoryItemReader oaiIdentifiersRepositoryItemReader,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      ItemProcessor<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>> oaiRecordAsyncItemProcessor,
      ItemWriter<Future<AbstractExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
      LoggingItemProcessListener<ExecutionRecordExternalIdentifier> loggingItemProcessListener) {
    return new StepBuilder(RECORDS_HARVEST_STEP_NAME, jobRepository)
        .<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(oaiIdentifiersRepositoryItemReader)
        .processor(oaiRecordAsyncItemProcessor)
        .listener(loggingItemProcessListener)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean
  ItemProcessor<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>> oaiRecordAsyncItemProcessor(
      ItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> oaiRecordItemProcessor,
      @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor taskExecutor) {
    AsyncItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(oaiRecordItemProcessor);
    asyncItemProcessor.setTaskExecutor(taskExecutor);
    return asyncItemProcessor;
  }

  @Bean
  TaskExecutor oaiHarvestStepAsyncTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
    executor.setCorePoolSize(parallelizeConfig.parallelizeSize());
    executor.setMaxPoolSize(parallelizeConfig.parallelizeSize());
    executor.initialize();
    return executor;
  }
}
