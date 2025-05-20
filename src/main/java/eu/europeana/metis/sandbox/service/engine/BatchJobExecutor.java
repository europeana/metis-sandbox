package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_BATCH_JOB_SUBTYPE;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_DATASET_ID;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_EXECUTION_ID;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_METADATA_PREFIX;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_OAI_ENDPOINT;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_OAI_SET;

import eu.europeana.metis.sandbox.batch.config.BatchJobType;
import eu.europeana.metis.sandbox.batch.config.OaiHarvestJobConfig;
import eu.europeana.metis.sandbox.batch.config.ValidationBatchBatchJobSubType;
import eu.europeana.metis.sandbox.batch.config.ValidationJobConfig;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import java.lang.invoke.MethodHandles;
import java.util.List;
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

  public BatchJobExecutor(List<? extends Job> jobs, JobLauncher jobLauncher, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      @Qualifier("pipelineTaskExecutor") TaskExecutor taskExecutor,
      DatasetRepository datasetRepository) {
    this.jobs = jobs;
    this.jobLauncher = jobLauncher;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.taskExecutor = taskExecutor;
    this.datasetRepository = datasetRepository;
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
        JobExecution validationExternalExecution = executeValidationExternal(datasetMetadata,
            harvestExecution.getJobId().toString());
        waitForCompletion(validationExternalExecution);
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
