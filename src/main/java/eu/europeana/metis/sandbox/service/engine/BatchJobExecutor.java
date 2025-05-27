package eu.europeana.metis.sandbox.service.engine;

import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_BATCH_JOB_SUBTYPE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_COMPRESSED_FILE_EXTENSION;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_COUNTRY;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_LANGUAGE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_DATASET_NAME;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_INPUT_FILE_PATH;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_METADATA_PREFIX;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_OAI_ENDPOINT;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_OAI_SET;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_SOURCE_EXECUTION_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_STEP_SIZE;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_TARGET_EXECUTION_ID;
import static eu.europeana.metis.sandbox.batch.common.ArgumentString.ARGUMENT_XSLT_CONTENT;

import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.TransformationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchJobSubType;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.config.batch.DebiasJobConfig;
import eu.europeana.metis.sandbox.config.batch.EnrichmentJobConfig;
import eu.europeana.metis.sandbox.config.batch.FileHarvestJobConfig;
import eu.europeana.metis.sandbox.config.batch.IndexingJobConfig;
import eu.europeana.metis.sandbox.config.batch.MediaJobConfig;
import eu.europeana.metis.sandbox.config.batch.NormalizationJobConfig;
import eu.europeana.metis.sandbox.config.batch.OaiHarvestJobConfig;
import eu.europeana.metis.sandbox.config.batch.TransformationJobConfig;
import eu.europeana.metis.sandbox.config.batch.ValidationJobConfig;
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetReportService;
import eu.europeana.metis.utils.CompressedFileExtension;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final TaskExecutor taskExecutor;
  private final DatasetRepository datasetRepository;
  private final TransformXsltRepository transformXsltRepository;
  private final DatasetReportService datasetReportService;

  //Those fields and handling is temporary until we have an external orchestrator(e.g. metis-core).
  final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> transformationToEdmExternalJobExecutionOrder = List.of(
      (datasetMetadata, previousJobExecution) -> executeTransformationToEdmExternal(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)));

  final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> validationJobExecutionOrder = List.of(
      (datasetMetadata, previousJobExecution) -> executeValidationExternal(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)),
      (datasetMetadata, previousJobExecution) -> executeTransformation(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)),
      (datasetMetadata, previousJobExecution) -> executeValidationInternal(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)));

  final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> afterValidationJobExecutionOrder = List.of(
      (datasetMetadata, previousJobExecution) -> executeNormalization(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)),
      (datasetMetadata, previousJobExecution) -> executeEnrichment(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)),
      (datasetMetadata, previousJobExecution) -> executeMedia(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)),
      (datasetMetadata, previousJobExecution) -> executeIndex(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)));

  final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> fullJobExecutionOrderWithTransformationToEdmExternal =
      Stream.of(transformationToEdmExternalJobExecutionOrder.stream(), validationJobExecutionOrder.stream(),
                afterValidationJobExecutionOrder.stream())
            .flatMap(Function.identity())
            .toList();

  final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> fullJobExecutionOrder =
      Stream.concat(validationJobExecutionOrder.stream(), afterValidationJobExecutionOrder.stream())
            .toList();


  final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> debiasJobExecutionOrder = List.of(
      (datasetMetadata, previousJobExecution) -> executeDebias(datasetMetadata,
          previousJobExecution.getJobParameters().getString(ARGUMENT_TARGET_EXECUTION_ID)));

  public BatchJobExecutor(List<? extends Job> jobs,
      @Qualifier("asyncJobLauncher") JobLauncher jobLauncher, JobExplorer jobExplorer,
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      @Qualifier("pipelineTaskExecutor") TaskExecutor taskExecutor,
      DatasetRepository datasetRepository, TransformXsltRepository transformXsltRepository,
      DatasetReportService datasetReportService) {
    this.jobs = jobs;
    this.jobLauncher = jobLauncher;
    this.jobExplorer = jobExplorer;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.taskExecutor = taskExecutor;
    this.datasetRepository = datasetRepository;
    this.transformXsltRepository = transformXsltRepository;
    this.datasetReportService = datasetReportService;
    LOGGER.info("Registered batch jobs: {}", jobs.stream().map(Job::getName).toList());
  }

  public void execute(DatasetMetadata datasetMetadata, String url, String setSpec, String metadataFormat, Integer stepSize,
      String xsltToEdmExternal) {
    taskExecutor.execute(() -> {
      final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> executionOrder;
      if (StringUtils.isBlank(xsltToEdmExternal)) {
        executionOrder = fullJobExecutionOrder;
      } else {
        executionOrder = fullJobExecutionOrderWithTransformationToEdmExternal;
      }
      JobExecution harvestExecution = executeOaiHarvest(datasetMetadata, url, setSpec, metadataFormat, stepSize);
      waitForCompletion(harvestExecution);

      if (harvestExecution.getStatus() == BatchStatus.COMPLETED) {
        long totalRecords = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(
            datasetMetadata.getDatasetId(),
            OaiHarvestJobConfig.BATCH_JOB.name());
        datasetRepository.updateRecordsQuantity(Integer.parseInt(datasetMetadata.getDatasetId()), totalRecords);

        JobExecution previousExecution = harvestExecution;
        for (BiFunction<DatasetMetadata, JobExecution, JobExecution> jobExecutionFunction : executionOrder) {
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

  public void execute(DatasetMetadata datasetMetadata, Path datasetRecordsCompressedFilePath,
      CompressedFileExtension compressedFileExtension, Integer stepSize, String xsltToEdmExternal) {
    taskExecutor.execute(() -> {
      final List<BiFunction<DatasetMetadata, JobExecution, JobExecution>> executionOrder;
      if (StringUtils.isBlank(xsltToEdmExternal)) {
        executionOrder = fullJobExecutionOrder;
      } else {
        executionOrder = fullJobExecutionOrderWithTransformationToEdmExternal;
      }
      JobExecution harvestExecution = executeFileHarvest(datasetMetadata, datasetRecordsCompressedFilePath,
          compressedFileExtension, stepSize);
      waitForCompletion(harvestExecution);

      if (harvestExecution.getStatus() == BatchStatus.COMPLETED) {
        long totalRecords = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(
            datasetMetadata.getDatasetId(),
            FileHarvestJobConfig.BATCH_JOB.name());
        datasetRepository.updateRecordsQuantity(Integer.parseInt(datasetMetadata.getDatasetId()), totalRecords);

        JobExecution previousExecution = harvestExecution;
        for (BiFunction<DatasetMetadata, JobExecution, JobExecution> jobExecutionFunction : executionOrder) {
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

  public void executeBlocking(DatasetMetadata datasetMetadata, Path recordFilePath) {
    JobExecution harvestExecution = executeFileHarvest(datasetMetadata, recordFilePath);
    waitForCompletion(harvestExecution);

    if (harvestExecution.getStatus() == BatchStatus.COMPLETED) {
      long totalRecords = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(
          datasetMetadata.getDatasetId(),
          FileHarvestJobConfig.BATCH_JOB.name());
      datasetRepository.updateRecordsQuantity(Integer.parseInt(datasetMetadata.getDatasetId()), totalRecords);

      JobExecution previousExecution = harvestExecution;
      for (BiFunction<DatasetMetadata, JobExecution, JobExecution> jobExecutionFunction : validationJobExecutionOrder) {
        JobExecution jobExecution = jobExecutionFunction.apply(datasetMetadata, previousExecution);
        waitForCompletion(jobExecution);
        if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
          throw new RuntimeException("Batch job " + jobExecution.getJobId() + " failed");
        }
        previousExecution = jobExecution;
      }
    }
  }

  public void execute(DatasetMetadata datasetMetadata) {
    taskExecutor.execute(() -> {
      JobExecution validationExecution = findJobInstance(datasetMetadata, ValidationJobConfig.BATCH_JOB,
          ValidationBatchJobSubType.INTERNAL);
      if (validationExecution == null) {
        throw new RuntimeException("Batch job not found");
      }

      if (validationExecution.getStatus() == BatchStatus.COMPLETED) {
        JobExecution previousExecution = validationExecution;
        for (BiFunction<DatasetMetadata, JobExecution, JobExecution> jobExecutionFunction : debiasJobExecutionOrder) {
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

  private @Nullable JobExecution findJobInstance(DatasetMetadata datasetMetadata, BatchJobType batchJobType,
      Enum<?> batchJobSubType) {
    List<JobInstance> jobInstances = new ArrayList<>();
    int start = 0;
    int pageSize = 100;
    List<JobInstance> page;

    do {
      page = jobExplorer.getJobInstances(batchJobType.name(), start, 100);
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
        boolean datasetMatches = Objects.equals(datasetId, datasetMetadata.getDatasetId());

        if (datasetMatches) {
          final boolean isMatchingWithoutSubtype = batchJobSubType == null && StringUtils.isBlank(jobSubTypeString);
          final boolean isMatchingWithSubType = batchJobSubType != null && batchJobSubType.name().equals(jobSubTypeString);
          if (isMatchingWithoutSubtype || isMatchingWithSubType) {
            matchingExecution = jobExecution;
          }
        }

      }
    }
    return matchingExecution;
  }

  private void waitForCompletion(JobExecution jobExecution) {
    try {
      while (jobExecution.isRunning()) {
        LOGGER.info("Job still running...");
        Thread.sleep(5000);
      }
      LOGGER.info("Job finished with status: {}", jobExecution.getStatus());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.error("Monitoring interrupted", e);
    }
  }

  private @NotNull JobExecution executeOaiHarvest(DatasetMetadata datasetMetadata, String url, String setSpec,
      String metadataFormat, Integer stepSize) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_OAI_ENDPOINT, url)
        .addString(ARGUMENT_OAI_SET, setSpec)
        .addString(ARGUMENT_METADATA_PREFIX, metadataFormat)
        .addString(ARGUMENT_STEP_SIZE, String.valueOf(stepSize))
        .toJobParameters();

    Job oaiHarvestJob = findJobByName(OaiHarvestJobConfig.BATCH_JOB);
    return runJob(oaiHarvestJob, jobParameters);
  }

  private @NotNull JobExecution executeFileHarvest(DatasetMetadata datasetMetadata, Path datasetRecordsCompressedFilePath,
      CompressedFileExtension compressedFileExtension, Integer stepSize) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_INPUT_FILE_PATH, datasetRecordsCompressedFilePath.toString())
        .addString(ARGUMENT_COMPRESSED_FILE_EXTENSION, compressedFileExtension.name())
        .addString(ARGUMENT_STEP_SIZE, String.valueOf(stepSize))
        .toJobParameters();

    Job oaiHarvestJob = findJobByName(FileHarvestJobConfig.BATCH_JOB);
    return runJob(oaiHarvestJob, jobParameters);
  }

  private @NotNull JobExecution executeFileHarvest(DatasetMetadata datasetMetadata, Path recordFilePath) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_INPUT_FILE_PATH, recordFilePath.toString())
        .toJobParameters();

    Job oaiHarvestJob = findJobByName(FileHarvestJobConfig.BATCH_JOB);
    return runJob(oaiHarvestJob, jobParameters);
  }

  private static @NotNull JobParameters getDefaultJobParameters(DatasetMetadata datasetMetadata) {
    return new JobParametersBuilder()
        .addString(ARGUMENT_TARGET_EXECUTION_ID, UUID.randomUUID().toString())
        .addString(ARGUMENT_DATASET_ID, datasetMetadata.getDatasetId())
        .toJobParameters();
  }

  private static @NotNull JobParameters getDefaultJobParameters(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata);
    return new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_SOURCE_EXECUTION_ID, sourceExecutionId)
        .toJobParameters();
  }

  private @NotNull JobExecution executeValidationExternal(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, ValidationBatchJobSubType.EXTERNAL.name())
        .toJobParameters();

    Job validationExternalJob = findJobByName(ValidationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeTransformation(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    Optional<TransformXsltEntity> transformXsltEntity = transformXsltRepository.findById(1);
    String transformXsltContent = transformXsltEntity.map(TransformXsltEntity::getTransformXslt).orElseThrow();

    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, TransformationBatchJobSubType.INTERNAL.name())
        .addString(ARGUMENT_DATASET_NAME, datasetMetadata.getDatasetName())
        .addString(ARGUMENT_DATASET_COUNTRY, datasetMetadata.getCountry().xmlValue())
        .addString(ARGUMENT_DATASET_LANGUAGE, datasetMetadata.getLanguage().name().toLowerCase(Locale.US))
        .addString(ARGUMENT_XSLT_CONTENT, transformXsltContent)
        .toJobParameters();

    Job validationExternalJob = findJobByName(TransformationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeTransformationToEdmExternal(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    final String transformXsltContent = datasetMetadata.getXsltToEdmExternal();
    if (StringUtils.isBlank(transformXsltContent)) {
      throw new IllegalArgumentException("xsltToEdmExternal is required");
    }

    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, TransformationBatchJobSubType.EXTERNAL.name())
        .addString(ARGUMENT_DATASET_NAME, datasetMetadata.getDatasetName())
        .addString(ARGUMENT_DATASET_COUNTRY, datasetMetadata.getCountry().xmlValue())
        .addString(ARGUMENT_DATASET_LANGUAGE, datasetMetadata.getLanguage().name().toLowerCase(Locale.US))
        .addString(ARGUMENT_XSLT_CONTENT, transformXsltContent)
        .toJobParameters();

    Job validationExternalJob = findJobByName(TransformationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeValidationInternal(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .addString(ARGUMENT_BATCH_JOB_SUBTYPE, ValidationBatchJobSubType.INTERNAL.name())
        .toJobParameters();

    Job validationExternalJob = findJobByName(ValidationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeNormalization(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .toJobParameters();

    Job validationExternalJob = findJobByName(NormalizationJobConfig.BATCH_JOB);
    return runJob(validationExternalJob, jobParameters);
  }

  private @NotNull JobExecution executeEnrichment(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .toJobParameters();

    Job enrichmentJob = findJobByName(EnrichmentJobConfig.BATCH_JOB);
    return runJob(enrichmentJob, jobParameters);
  }

  private @NotNull JobExecution executeMedia(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .toJobParameters();

    Job mediaJob = findJobByName(MediaJobConfig.BATCH_JOB);
    return runJob(mediaJob, jobParameters);
  }

  private @NotNull JobExecution executeIndex(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .toJobParameters();

    Job indexJob = findJobByName(IndexingJobConfig.BATCH_JOB);
    return runJob(indexJob, jobParameters);
  }

  private @NotNull JobExecution executeDebias(DatasetMetadata datasetMetadata, String sourceExecutionId) {
    JobParameters defaultJobParameters = getDefaultJobParameters(datasetMetadata, sourceExecutionId);
    JobParameters jobParameters = new JobParametersBuilder(defaultJobParameters)
        .toJobParameters();

    Job debiasJob = findJobByName(DebiasJobConfig.BATCH_JOB);
    return runJob(debiasJob, jobParameters);
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
