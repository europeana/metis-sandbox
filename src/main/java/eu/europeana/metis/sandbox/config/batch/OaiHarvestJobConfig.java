package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_OAI;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.AbstractExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.reader.ExternalIdentifiersRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.reader.OaiIdentifiersItemReader;
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
 * Configuration class for the Oai Harvest Job, responsible for defining the batch job, its step, and components.
 */
@Slf4j
@Configuration
public class OaiHarvestJobConfig {

  public static final BatchJobType BATCH_JOB = HARVEST_OAI;
  public static final String IDENTIFIERS_HARVEST_STEP_NAME = "oaiIdentifiersHarvest";
  public static final String RECORDS_HARVEST_STEP_NAME = "oaiRecordsHarvest";
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
      OaiIdentifiersItemReader oaiIdentifiersItemReader,
      ExternalIdentifiersItemWriter externalIdentifiersItemWriter,
      JobRepository jobRepository,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {

    return new StepBuilder(IDENTIFIERS_HARVEST_STEP_NAME, jobRepository)
        .<ExecutionRecordExternalIdentifier, ExecutionRecordExternalIdentifier>chunk(parallelizeConfig.chunkSize(), transactionManager)
        .reader(oaiIdentifiersItemReader)
        .writer(externalIdentifiersItemWriter)
        .build();
  }

  @Bean(RECORDS_HARVEST_STEP_NAME)
  Step oaiRecordsHarvestStep(
      JobRepository jobRepository,
      ExternalIdentifiersRepositoryItemReader externalIdentifiersRepositoryItemReader,
      @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
      @Qualifier("oaiRecordAsyncItemProcessor")
      ItemProcessor<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>> oaiRecordAsyncItemProcessor,
      ItemWriter<Future<AbstractExecutionRecordDTO>> executionRecordDTOAsyncItemWriter) {
    return new StepBuilder(RECORDS_HARVEST_STEP_NAME, jobRepository)
        .<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>>chunk(parallelizeConfig.chunkSize(),
            transactionManager)
        .reader(externalIdentifiersRepositoryItemReader)
        .processor(oaiRecordAsyncItemProcessor)
        .writer(executionRecordDTOAsyncItemWriter)
        .build();
  }

  @Bean("oaiRecordAsyncItemProcessor")
  ItemProcessor<ExecutionRecordExternalIdentifier, Future<AbstractExecutionRecordDTO>> oaiRecordAsyncItemProcessor(
      @Qualifier("oaiRecordHarvestItemProcessor") ItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> oaiRecordHarvestItemProcessor,
      @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor taskExecutor) {
    AsyncItemProcessor<ExecutionRecordExternalIdentifier, AbstractExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(oaiRecordHarvestItemProcessor);
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
