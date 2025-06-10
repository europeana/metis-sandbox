package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_BATCH_JOB_SUBTYPE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_COUNTRY;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_LANGUAGE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_NAME;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_HARVEST_PARAMETER_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_SOURCE_EXECUTION_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_STEP_SIZE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_TARGET_EXECUTION_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_XSLT_ID;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.DEBIAS;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.ENRICH;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.HARVEST_FILE;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.HARVEST_OAI;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.INDEX_PUBLISH;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.MEDIA;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.NORMALIZE;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.TRANSFORM_EXTERNAL;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.TRANSFORM_INTERNAL;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.VALIDATE_EXTERNAL;
import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.VALIDATE_INTERNAL;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.awaitility.Awaitility.await;

import eu.europeana.metis.sandbox.batch.common.BatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.common.IndexBatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.common.DatasetMetadata;
import eu.europeana.metis.sandbox.common.ExecutionMetadata;
import eu.europeana.metis.sandbox.common.InputMetadata;
import eu.europeana.metis.sandbox.config.batch.DebiasJobConfig;
import eu.europeana.metis.sandbox.config.batch.EnrichJobConfig;
import eu.europeana.metis.sandbox.config.batch.FileHarvestJobConfig;
import eu.europeana.metis.sandbox.config.batch.IndexJobConfig;
import eu.europeana.metis.sandbox.config.batch.MediaJobConfig;
import eu.europeana.metis.sandbox.config.batch.NormalizeJobConfig;
import eu.europeana.metis.sandbox.config.batch.OaiHarvestJobConfig;
import eu.europeana.metis.sandbox.config.batch.TransformJobConfig;
import eu.europeana.metis.sandbox.config.batch.ValidationJobConfig;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.XsltType;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.awaitility.core.ConditionEvaluationListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.explore.JobExplorer;
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
  private final JobExplorer jobExplorer;
  private final TaskExecutor taskExecutor;
  private final TransformXsltRepository transformXsltRepository;

  private final EnumMap<FullBatchJobType, Function<ExecutionMetadata, JobExecution>> jobExecutorsByType;

  public BatchJobExecutor(List<? extends Job> jobs,
      @Qualifier("asyncJobLauncher") JobLauncher jobLauncher, JobExplorer jobExplorer,
      @Qualifier("pipelineTaskExecutor") TaskExecutor taskExecutor, TransformXsltRepository transformXsltRepository) {
    this.jobs = jobs;
    this.jobLauncher = jobLauncher;
    this.jobExplorer = jobExplorer;
    this.taskExecutor = taskExecutor;
    this.transformXsltRepository = transformXsltRepository;
    LOGGER.info("Registered batch workflow: {}", jobs.stream().map(Job::getName).toList());

    this.jobExecutorsByType = new EnumMap<>(FullBatchJobType.class);
    this.jobExecutorsByType.put(HARVEST_OAI, this::runOaiHarvest);
    this.jobExecutorsByType.put(HARVEST_FILE, this::runFileHarvest);
    this.jobExecutorsByType.put(TRANSFORM_EXTERNAL, this::executeTransformToEdmExternal);
    this.jobExecutorsByType.put(VALIDATE_EXTERNAL, this::executeValidateExternal);
    this.jobExecutorsByType.put(TRANSFORM_INTERNAL, this::executeTransformInternal);
    this.jobExecutorsByType.put(VALIDATE_INTERNAL, this::executeValidateInternal);
    this.jobExecutorsByType.put(NORMALIZE, this::executeNormalize);
    this.jobExecutorsByType.put(ENRICH, this::executeEnrich);
    this.jobExecutorsByType.put(MEDIA, this::executeMedia);
    this.jobExecutorsByType.put(INDEX_PUBLISH, this::executeIndexPublish);
    this.jobExecutorsByType.put(DEBIAS, this::executeDebias);
  }

  public void execute(ExecutionMetadata executionMetadata) {
    taskExecutor.execute(() -> executeSteps(executionMetadata));
  }

  public void executeBlocking(ExecutionMetadata executionMetadata) {
    executeSteps(executionMetadata);
  }

  public void executeDebiasWorkflow(ExecutionMetadata executionMetadata) {
    JobExecution validationExecution = findJobInstance(executionMetadata, FullBatchJobType.VALIDATE_INTERNAL).orElseThrow();
    InputMetadata inputMetadata = new InputMetadata(
        validationExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID));
    ExecutionMetadata currentExecutionMetadata = ExecutionMetadata.builder()
                                                                  .datasetMetadata(executionMetadata.getDatasetMetadata())
                                                                  .inputMetadata(inputMetadata)
                                                                  .build();
    execute(currentExecutionMetadata);
  }

  private void executeSteps(ExecutionMetadata executionMetadata) {
    ExecutionMetadata currentExecutionMetadata = executionMetadata;

    for (FullBatchJobType step : WorkflowHelper.getWorkflow(executionMetadata)) {

      Function<ExecutionMetadata, JobExecution> executor = jobExecutorsByType.get(step);
      if (executor == null) {
        throw new IllegalStateException("No executor for step: " + step);
      }

      JobExecution execution = executor.apply(currentExecutionMetadata);
      waitForCompletion(execution);
      if (execution.getStatus() != BatchStatus.COMPLETED) {
        throw new RuntimeException("Step failed: " + step);
      }
      currentExecutionMetadata = ExecutionMetadata.builder().datasetMetadata(executionMetadata.getDatasetMetadata())
                                                  .inputMetadata(new InputMetadata(
                                                      execution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID),
                                                      currentExecutionMetadata.getInputMetadata()))
                                                  .build();
    }
  }

  private Optional<JobExecution> findJobInstance(ExecutionMetadata executionMetadata, FullBatchJobType fullBatchJobType) {
    List<JobInstance> jobInstances = new ArrayList<>();
    int start = 0;
    int pageSize = 100;
    List<JobInstance> page;

    do {
      page = jobExplorer.getJobInstances(fullBatchJobType.getBatchJobType().name(), start, pageSize);
      jobInstances.addAll(page);
      start += pageSize;
    } while (!page.isEmpty());

    JobExecution matchingExecution = null;
    for (JobInstance jobInstance : jobInstances) {
      List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
      for (JobExecution jobExecution : jobExecutions) {
        JobParameters jobParameters = jobExecution.getJobParameters();
        String jobSubTypeString = jobParameters.getString(ARGUMENT_BATCH_JOB_SUBTYPE);
        String datasetId = jobParameters.getString(ARGUMENT_DATASET_ID);
        boolean datasetMatches = Objects.equals(datasetId, executionMetadata.getDatasetMetadata().getDatasetId());

        if (datasetMatches) {
          BatchJobSubType batchJobSubType = fullBatchJobType.getBatchJobSubType();
          final boolean isMatchingWithoutSubtype = batchJobSubType == null && isBlank(jobSubTypeString);
          final boolean isMatchingWithSubType = batchJobSubType != null && batchJobSubType.name().equals(jobSubTypeString);
          if (isMatchingWithoutSubtype || isMatchingWithSubType) {
            matchingExecution = jobExecution;
          }
        }

      }
    }
    return Optional.ofNullable(matchingExecution);
  }

  private void waitForCompletion(JobExecution jobExecution) {
    try {
      await().atMost(1, DAYS)
             .pollInterval(5, SECONDS)
             .conditionEvaluationListener((ConditionEvaluationListener<Object>) condition ->
                 LOGGER.info("Job Id: {}, status: {}, isRunning: {}", jobExecution.getJobId(), jobExecution.getStatus(),
                     jobExecution.isRunning()))
             .until(() -> !jobExecution.isRunning());
      LOGGER.info("Job finished with status: {}", jobExecution.getStatus());
    } catch (Exception e) {
      LOGGER.error("Error while waiting for job completion", e);
      throw new RuntimeException(e);
    }
  }

  private @NotNull JobExecution runOaiHarvest(ExecutionMetadata executionMetadata) {
    InputMetadata inputMetadata = executionMetadata.getInputMetadata();
    JobParameters stepParameters = new JobParametersBuilder()
        .addString(ARGUMENT_HARVEST_PARAMETER_ID, inputMetadata.getHarvestParametersEntity().getId().toString())
        .addString(ARGUMENT_STEP_SIZE, String.valueOf(inputMetadata.getHarvestParametersEntity().getStepSize()))
        .toJobParameters();

    return prepareAndRunJob(OaiHarvestJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution runFileHarvest(ExecutionMetadata executionMetadata) {
    InputMetadata inputMetadata = executionMetadata.getInputMetadata();
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
        .addString(ARGUMENT_HARVEST_PARAMETER_ID, inputMetadata.getHarvestParametersEntity().getId().toString())
        .addString(ARGUMENT_STEP_SIZE, String.valueOf(inputMetadata.getHarvestParametersEntity().getStepSize()));
    JobParameters stepParameters = jobParametersBuilder.toJobParameters();

    return prepareAndRunJob(FileHarvestJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution executeValidateExternal(ExecutionMetadata executionMetadata) {
    JobParameters stepParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, ValidationBatchJobSubType.EXTERNAL.name())
        .toJobParameters();

    return prepareAndRunJob(ValidationJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution executeTransformInternal(ExecutionMetadata executionMetadata) {
    Optional<TransformXsltEntity> transformXsltEntity = transformXsltRepository.findFirstByTypeOrderById(XsltType.DEFAULT);
    String transformXsltId = transformXsltEntity.map(TransformXsltEntity::getId).map(String::valueOf).orElseThrow();

    DatasetMetadata datasetMetadata = executionMetadata.getDatasetMetadata();
    JobParameters stepParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, TransformationBatchJobSubType.INTERNAL.name())
        .addString(ARGUMENT_DATASET_NAME, datasetMetadata.getDatasetName())
        .addString(ARGUMENT_DATASET_COUNTRY, datasetMetadata.getCountry().xmlValue())
        .addString(ARGUMENT_DATASET_LANGUAGE, datasetMetadata.getLanguage().name().toLowerCase(Locale.US))
        .addString(ARGUMENT_XSLT_ID, transformXsltId)
        .toJobParameters();

    return prepareAndRunJob(TransformJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution executeTransformToEdmExternal(ExecutionMetadata executionMetadata) {
    String transformXsltId = String.valueOf(executionMetadata.getInputMetadata().getTransformXsltEntity().getId());
    DatasetMetadata datasetMetadata = executionMetadata.getDatasetMetadata();

    JobParameters stepParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, TransformationBatchJobSubType.EXTERNAL.name())
        .addString(ARGUMENT_DATASET_NAME, datasetMetadata.getDatasetName())
        .addString(ARGUMENT_DATASET_COUNTRY, datasetMetadata.getCountry().xmlValue())
        .addString(ARGUMENT_DATASET_LANGUAGE, datasetMetadata.getLanguage().name().toLowerCase(Locale.US))
        .addString(ARGUMENT_XSLT_ID, transformXsltId)
        .toJobParameters();

    return prepareAndRunJob(TransformJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution executeValidateInternal(ExecutionMetadata executionMetadata) {
    JobParameters stepParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, ValidationBatchJobSubType.INTERNAL.name())
        .toJobParameters();

    return prepareAndRunJob(ValidationJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution executeNormalize(ExecutionMetadata executionMetadata) {
    return prepareAndRunJob(NormalizeJobConfig.BATCH_JOB, executionMetadata);
  }

  private @NotNull JobExecution executeEnrich(ExecutionMetadata executionMetadata) {
    return prepareAndRunJob(EnrichJobConfig.BATCH_JOB, executionMetadata);
  }

  private @NotNull JobExecution executeMedia(ExecutionMetadata executionMetadata) {
    return prepareAndRunJob(MediaJobConfig.BATCH_JOB, executionMetadata);
  }

  private @NotNull JobExecution executeIndexPublish(ExecutionMetadata executionMetadata) {
    JobParameters stepParameters = new JobParametersBuilder()
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, IndexBatchJobSubType.PUBLISH.name())
        .toJobParameters();
    return prepareAndRunJob(IndexJobConfig.BATCH_JOB, executionMetadata, stepParameters);
  }

  private @NotNull JobExecution executeDebias(ExecutionMetadata executionMetadata) {
    return prepareAndRunJob(DebiasJobConfig.BATCH_JOB, executionMetadata);
  }

  private JobExecution prepareAndRunJob(BatchJobType batchJobType, ExecutionMetadata executionMetadata) {
    return prepareAndRunJob(batchJobType, executionMetadata, new JobParameters());
  }

  private JobExecution prepareAndRunJob(BatchJobType batchJobType, ExecutionMetadata executionMetadata,
      JobParameters jobParameters) {
    Job job = findJobByName(batchJobType);
    JobParameters defaultJobParameters = getDefaultJobParameters(executionMetadata);
    JobParameters finalJobParameters = new JobParametersBuilder(defaultJobParameters)
        .addJobParameters(jobParameters)
        .toJobParameters();

    return runJob(job, finalJobParameters);
  }

  private Job findJobByName(BatchJobType batchJobType) {
    return jobs.stream()
               .filter(job -> job.getName().equals(batchJobType.name()))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("No job found with name: " + batchJobType.name()));
  }

  private static @NotNull JobParameters getDefaultJobParameters(DatasetMetadata datasetMetadata) {
    return new JobParametersBuilder()
        .addString(ARGUMENT_TARGET_EXECUTION_ID, UUID.randomUUID().toString())
        .addString(ARGUMENT_DATASET_ID, datasetMetadata.getDatasetId())
        .toJobParameters();
  }

  private static @NotNull JobParameters getDefaultJobParameters(ExecutionMetadata executionMetadata) {
    DatasetMetadata datasetMetadata = executionMetadata.getDatasetMetadata();
    InputMetadata inputMetadata = executionMetadata.getInputMetadata();
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata);
    if (isBlank(inputMetadata.getSourceExecutionId())) {
      return defaultJobParameters;
    }
    return new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_SOURCE_EXECUTION_ID, inputMetadata.getSourceExecutionId())
        .toJobParameters();
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

}
