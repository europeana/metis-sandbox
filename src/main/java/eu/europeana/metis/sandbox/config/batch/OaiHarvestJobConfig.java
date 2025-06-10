package eu.europeana.metis.sandbox.config.batch;

import static eu.europeana.metis.sandbox.batch.common.BatchJobType.HARVEST_OAI;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.dto.ExecutionRecordDTO;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordExternalIdentifier;
import eu.europeana.metis.sandbox.batch.processor.listener.LoggingItemProcessListener;
import eu.europeana.metis.sandbox.batch.reader.OaiIdentifiersEndpointItemReader;
import eu.europeana.metis.sandbox.batch.reader.OaiIdentifiersRepositoryItemReader;
import eu.europeana.metis.sandbox.batch.writer.OaiIdentifiersWriter;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.Future;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class OaiHarvestJobConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final BatchJobType BATCH_JOB = HARVEST_OAI;
    public static final String IDENTIFIERS_HARVEST_STEP_NAME = "identifiersHarvest";
    public static final String RECORDS_HARVEST_STEP_NAME = "recordsHarvest";

    @Value("${oaiHarvest.chunkSize:5}")
    public int chunkSize;
    @Value("${oaiHarvest.parallelizationSize:1}")
    public int parallelization;

    @Bean
    public Job oaiHarvestJob(
            JobRepository jobRepository,
            @Qualifier(IDENTIFIERS_HARVEST_STEP_NAME) Step identifiersHarvestStep,
            @Qualifier(RECORDS_HARVEST_STEP_NAME) Step recordsHarvestStep) {

        LOGGER.info("Chunk size: {}, Parallelization size: {}", chunkSize, parallelization);
        return new JobBuilder(BATCH_JOB.name(), jobRepository)
                .start(identifiersHarvestStep)
                .next(recordsHarvestStep)
                .build();
    }

    @Bean(IDENTIFIERS_HARVEST_STEP_NAME)
    public Step oaidentifiersEndpointHarvestStep(
            OaiIdentifiersEndpointItemReader oaiIdentifiersEndpointItemReader,
            OaiIdentifiersWriter oaiIdentifiersWriter,
            JobRepository jobRepository,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor oaiHarvestStepAsyncTaskExecutor) {

        return new StepBuilder(IDENTIFIERS_HARVEST_STEP_NAME, jobRepository)
                .<ExecutionRecordExternalIdentifier, ExecutionRecordExternalIdentifier>chunk(chunkSize, transactionManager)
                .reader(oaiIdentifiersEndpointItemReader)
                .writer(oaiIdentifiersWriter)
//                .taskExecutor(oaiHarvestStepAsyncTaskExecutor)
                .build();
    }

    @Bean(RECORDS_HARVEST_STEP_NAME)
    public Step oaiRecordsHarvestStep(
            JobRepository jobRepository,
            OaiIdentifiersRepositoryItemReader oaiIdentifiersRepositoryItemReader,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            ItemProcessor<ExecutionRecordExternalIdentifier, Future<ExecutionRecordDTO>> oaiRecordAsyncItemProcessor,
            ItemWriter<Future<ExecutionRecordDTO>> executionRecordDTOAsyncItemWriter,
            LoggingItemProcessListener<ExecutionRecordExternalIdentifier> loggingItemProcessListener) {
        return new StepBuilder(RECORDS_HARVEST_STEP_NAME, jobRepository)
                .<ExecutionRecordExternalIdentifier, Future<ExecutionRecordDTO>>chunk(chunkSize, transactionManager)
                .reader(oaiIdentifiersRepositoryItemReader)
                .processor(oaiRecordAsyncItemProcessor)
                .listener(loggingItemProcessListener)
                .writer(executionRecordDTOAsyncItemWriter)
                .build();
    }

    @Bean
    public ItemProcessor<ExecutionRecordExternalIdentifier, Future<ExecutionRecordDTO>> oaiRecordAsyncItemProcessor(
            ItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> oaiRecordItemProcessor,
            @Qualifier("oaiHarvestStepAsyncTaskExecutor") TaskExecutor taskExecutor) {
        AsyncItemProcessor<ExecutionRecordExternalIdentifier, ExecutionRecordDTO> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(oaiRecordItemProcessor);
        asyncItemProcessor.setTaskExecutor(taskExecutor);
        return asyncItemProcessor;
    }

    @Bean
    public TaskExecutor oaiHarvestStepAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(BATCH_JOB.name() + "-");
        executor.setCorePoolSize(parallelization);
        executor.setMaxPoolSize(parallelization);
        executor.initialize();
        return executor;
    }
}
