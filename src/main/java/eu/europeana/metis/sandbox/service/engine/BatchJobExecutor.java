package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_BATCH_JOB_SUBTYPE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_COUNTRY;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_LANGUAGE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_NAME;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_EXECUTION_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_METADATA_PREFIX;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_OAI_ENDPOINT;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_OAI_SET;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_XSLT_CONTENT;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.config.batch.OaiHarvestJobConfig;
import eu.europeana.metis.sandbox.config.batch.TransformationJobConfig;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchBatchJobSubType;
import eu.europeana.metis.sandbox.config.batch.ValidationJobConfig;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class BatchJobExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final List<? extends Job> jobs;
  private final JobLauncher jobLauncher;
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final TaskExecutor taskExecutor;
  private final DatasetRepository datasetRepository;
  private final TransformXsltRepository transformXsltRepository;

  List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> jobExecutionOrder = List.of(
      (datasetMetadata, previousJobExecution) -> executeValidationExternal(datasetMetadata,
          previousJobExecution.getJobId().toString()),
      (datasetMetadata, previousJobExecution) -> executeTransformation(datasetMetadata,
          previousJobExecution.getJobId().toString()),
      (datasetMetadata, previousJobExecution) -> executeValidationInternal(datasetMetadata,
          previousJobExecution.getJobId().toString()));

  public BatchJobExecutor(List<? extends Job> jobs,
      @Qualifier("asyncJobLauncher") JobLauncher jobLauncher, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      @Qualifier("pipelineTaskExecutor") TaskExecutor taskExecutor,
      DatasetRepository datasetRepository, TransformXsltRepository transformXsltRepository) {
    this.jobs = jobs;
    this.jobLauncher = jobLauncher;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.taskExecutor = taskExecutor;
    this.datasetRepository = datasetRepository;
    this.transformXsltRepository = transformXsltRepository;
    LOGGER.info("Registered batch jobs: {}", jobs.stream().map(Job::getName).toList());
  }

  public void execute(DatasetMetadata datasetMetadata, String url, String setSpec, String metadataFormat) {
    taskExecutor.execute(() -> {
      JobExecution harvestExecution = executeOaiHarvest(datasetMetadata, url, setSpec, metadataFormat);
      waitForCompletion(harvestExecution);

      if (harvestExecution.getStatus() == BatchStatus.COMPLETED) {
        long totalRecords = executionRecordRepository.countByIdentifier_DatasetIdAndExecutionName(datasetMetadata.getDatasetId(),
            OaiHarvestJobConfig.BATCH_JOB.name());
        datasetRepository.updateRecordsQuantity(Integer.parseInt(datasetMetadata.getDatasetId()), totalRecords);

        JobExecution previousExecution = harvestExecution;
        for (BiFunction<DatasetMetadata, JobExecution, JobExecution> jobExecutionFunction : jobExecutionOrder) {
          JobExecution jobExecution = jobExecutionFunction.apply(datasetMetadata, previousExecution);
          waitForCompletion(jobExecution);
          if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
            throw new RuntimeException("Batch job " + jobExecution.getJobId() + " failed");
          }
          previousExecution = jobExecution;
        }
      }
    });
  }

  private void waitForCompletion(JobExecution jobExecution) {
    try {
      while (jobExecution.isRunning()) {
        LOGGER.info("Job still running...");
        Thread.sleep(1000);
      }
      LOGGER.info("Job finished with status: {}", jobExecution.getStatus());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.error("Monitoring interrupted", e);
    }
  }

  private @NotNull JobExecution executeOaiHarvest(DatasetMetadata datasetMetadata, String url, String setSpec,
      String metadataFormat) {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString(ARGUMENT_OAI_ENDPOINT, url)
        .addString(ARGUMENT_OAI_SET, setSpec)
        .addString(ARGUMENT_METADATA_PREFIX, metadataFormat)
        .addString(ARGUMENT_DATASET_ID, datasetMetadata.getDatasetId())
        .toJobParameters();

    Job oaiHarvestJob = findJobByName(OaiHarvestJobConfig.BATCH_JOB);
    return runJob(oaiHarvestJob, jobParameters);
  }

  private @NotNull JobExecution executeValidationExternal(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, ValidationBatchBatchJobSubType.EXTERNAL.getName())
        .addString(ARGUMENT_DATASET_ID, datasetMetadata.getDatasetId())
        .addString(ARGUMENT_EXECUTION_ID, sourceExecutionId)
        .toJobParameters();

    Job validationExternalJob = findJobByName(ValidationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeTransformation(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    Optional<TransformXsltEntity> transformXsltEntity = transformXsltRepository.findById(1);
    String transformXsltContent = transformXsltEntity.map(TransformXsltEntity::getTransformXslt).orElseThrow();

    JobParameters jobParameters = new JobParametersBuilder()
        .addString(ARGUMENT_DATASET_ID, datasetMetadata.getDatasetId())
        .addString(ARGUMENT_EXECUTION_ID, sourceExecutionId)
        .addString(ARGUMENT_DATASET_NAME, datasetMetadata.getDatasetName())
        .addString(ARGUMENT_DATASET_COUNTRY, datasetMetadata.getCountry().xmlValue())
        .addString(ARGUMENT_DATASET_LANGUAGE, datasetMetadata.getLanguage().name().toLowerCase())
        .addString(ARGUMENT_XSLT_CONTENT, transformXsltContent)
        .toJobParameters();

    Job validationExternalJob = findJobByName(TransformationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeValidationInternal(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters jobParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, ValidationBatchBatchJobSubType.INTERNAL.getName())
        .addString(ARGUMENT_DATASET_ID, datasetMetadata.getDatasetId())
        .addString(ARGUMENT_EXECUTION_ID, sourceExecutionId)
        .toJobParameters();

    Job validationExternalJob = findJobByName(ValidationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution runJob(Job oaiHarvestJob, JobParameters jobParameters) {
    JobExecution jobExecution;
    try {
      jobExecution = jobLauncher.run(oaiHarvestJob, jobParameters);
    } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException |
             JobParametersInvalidException e) {
      throw new RuntimeException(e);
    }
    return jobExecution;
  }

  private Job findJobByName(BatchJobType batchJobType) {
    return jobs.stream()
               .filter(job -> job.getName().equals(batchJobType.name()))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("No job found with name: " + batchJobType.name()));
  }

}
