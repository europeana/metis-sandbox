package eu.europeana.metis.sandbox.batch.config;

import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_DATASET_ID;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_METADATA_PREFIX;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_OAI_ENDPOINT;
import static eu.europeana.metis.sandbox.batch.config.ArgumentString.ARGUMENT_OAI_SET;
import static java.lang.String.format;

import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import jakarta.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

//todo: remove this, it's java for testing
@Component
@EnableAsync
public class BatchJobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final List<? extends Job> jobs;
  private final JobLauncher jobLauncher;
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;

  public BatchJobService(List<? extends Job> jobs,
      @Qualifier("asyncJobLauncher") JobLauncher jobLauncher, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository) {
    this.jobs = jobs;
    this.jobLauncher = jobLauncher;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
  }

  //todo: Add a job to run here and try to monitor it until it ends.
//  @PostConstruct
//  public void logRegisteredJobs()
//      throws JobInstanceAlreadyCompleteException,
//      JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, InterruptedException {
//    LOGGER.info("Registered batch jobs: {}", jobs.stream().map(Job::getName).toList());
//
//    String uuid = UUID.randomUUID().toString();
//    JobParameters jobParameters = new JobParametersBuilder()
//        .addString(ARGUMENT_OAI_ENDPOINT, "https://dnet-prod.efg.d4science.org/efg/mvc/oai/oai.do")
//        .addString(ARGUMENT_OAI_SET, "ivac")
//        .addString(ARGUMENT_METADATA_PREFIX, "edm")
//        .addString(ARGUMENT_DATASET_ID, "1-" + uuid)
//        .addString("UUID", uuid)
//        .toJobParameters();
//
//    Job oaiHarvestJob = jobs.stream().filter(job -> job.getName().equals(OaiHarvestJobConfig.BATCH_JOB)).findFirst()
//                            .orElseThrow();
//
//    JobExecution jobExecution = jobLauncher.run(oaiHarvestJob, jobParameters);
//
//    monitorJobExecution(jobExecution);
//  }

  //todo: to be removed, this is not really necessary since the monitor check comes from http request.
  @Async
  public void monitorJobExecution(JobExecution jobExecution) {
    try {
      while (jobExecution.isRunning()) {
        LOGGER.info("Job still running...");
        printProgress(jobExecution);
        Thread.sleep(1000);
      }
      LOGGER.info("Job finished with status: {}", jobExecution.getStatus());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.error("Monitoring interrupted", e);
    }
  }

  private void printProgress(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    String datasetId = jobExecution.getJobParameters().getString(ARGUMENT_DATASET_ID);
    String executionId = jobExecution.getJobId().toString();
    String uuid = jobExecution.getJobParameters().getString("UUID");
    LOGGER.info("=================================================");
    LOGGER.info("Job {} with parameters {}.", jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
    LOGGER.info("DatasetId: {}, UUID: {}", datasetId, uuid);
    LOGGER.info(jobExecution.getStatus().toString());
    LOGGER.info(jobExecution.getStepExecutions().toString());
    LOGGER.info(executionId);

    final long sourceTotal;
    if (jobName.equals(OaiHarvestJobConfig.BATCH_JOB)) {
      sourceTotal = executionRecordExternalIdentifierRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionId(datasetId, executionId);
    } else {
      sourceTotal = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionId(datasetId, "sourceExecutionId");
    }


    final long successProcessed =
        executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionId(datasetId, executionId);
    final long exceptions =
        executionRecordExceptionLogRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionId(datasetId, executionId);
    final long processed = successProcessed + exceptions;

    LOGGER.info(format("Task progress - Processed/SourceTotal: %s/%s, Exceptions: %s", processed, sourceTotal, exceptions));

  }
}
