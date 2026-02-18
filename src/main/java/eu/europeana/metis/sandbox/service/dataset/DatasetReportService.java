package eu.europeana.metis.sandbox.service.dataset;

import static eu.europeana.metis.sandbox.batch.common.FullBatchJobType.BatchJobGroup.HARVEST;
import static java.lang.String.format;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRun;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordErrorRepository.ExecutionRecordErrorProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository.ExecutionRecordIdentifierProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningRepository.ExecutionRecordWarningProjection;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRunRepository;
import eu.europeana.metis.sandbox.common.HarvestParametersConverter;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTaskProgress;
import eu.europeana.metis.sandbox.controller.task.input.SandboxTaskProgress.SandboxTaskState;
import eu.europeana.metis.sandbox.dto.DatasetInfoDTO;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import eu.europeana.metis.sandbox.dto.report.DatasetErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressByStepDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import eu.europeana.metis.sandbox.dto.report.TierStatisticsDTO;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfoDTO;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.TransformXsltEntity;
import eu.europeana.metis.sandbox.entity.harvest.HarvestParametersEntity;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.DatasetRepository.DatasetIdProjection;
import eu.europeana.metis.sandbox.repository.TransformXsltRepository;
import eu.europeana.metis.sandbox.service.engine.BatchJobExecutor;
import eu.europeana.metis.sandbox.service.engine.WorkflowHelper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing dataset reports and processing statuses.
 */
@Service
@Slf4j
public class DatasetReportService {

  private static final String HARVESTING_IDENTIFIERS_MESSAGE = "Harvesting dataset identifiers and records.";
  private static final String PROCESSING_DATASET_MESSAGE = "A review URL will be generated when the dataset has finished processing.";
  private static final String SEPARATOR = "_";
  private static final String SUFFIX = "*";
  @Value("${sandbox.portal.publish.dataset-base-url}")
  private String portalPublishDatasetUrl;

  private final DatasetRepository datasetRepository;
  private final TransformXsltRepository transformXsltRepository;
  private final ExecutionRunRepository executionRunRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordErrorRepository executionRecordErrorRepository;
  private final ExecutionRecordWarningRepository executionRecordWarningRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;
  private final HarvestParameterService harvestParameterService;
  private final BatchJobExecutor batchJobExecutor;

  /**
   * Constructor.
   *
   * @param datasetRepository repository interface for accessing dataset-related data
   * @param executionRecordRepository repository interface for managing execution records
   * @param executionRecordErrorRepository repository interface for managing execution record errors
   * @param executionRecordWarningRepository repository interface for managing execution record warnings
   * @param executionRecordTierContextRepository repository interface for managing execution record tier context
   * @param transformXsltRepository repository interface for managing XSLT transformations
   * @param harvestParameterService service for handling harvest parameters
   */
  public DatasetReportService(
      DatasetRepository datasetRepository, ExecutionRunRepository executionRunRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordErrorRepository executionRecordErrorRepository,
      ExecutionRecordWarningRepository executionRecordWarningRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository,
      TransformXsltRepository transformXsltRepository, HarvestParameterService harvestParameterService,
      BatchJobExecutor batchJobExecutor) {
    this.datasetRepository = datasetRepository;
    this.executionRunRepository = executionRunRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordErrorRepository = executionRecordErrorRepository;
    this.executionRecordWarningRepository = executionRecordWarningRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
    this.transformXsltRepository = transformXsltRepository;
    this.harvestParameterService = harvestParameterService;
    this.batchJobExecutor = batchJobExecutor;
  }

  /**
   * Finds dataset IDs created before a specified number of days.
   *
   * @param days the number of days before which the datasets were created
   * @return a list of dataset IDs created before the specified number of days
   */
  public List<String> findDatasetIdsByCreatedBefore(int days) {
    ZonedDateTime retentionDate = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(days);

    try {
      return datasetRepository.findByCreatedDateBefore(retentionDate).stream()
                              .map(DatasetIdProjection::getDatasetId)
                              .map(Object::toString)
                              .toList();
    } catch (RuntimeException e) {
      throw new ServiceException(format("Error getting datasets older than %s days. ", days), e);
    }
  }

  /**
   * Retrieves dataset information for the specified dataset ID.
   *
   * <p>Fetches and processes data from multiple repositories and services to assemble a complete
   * dataset information object.
   *
   * @param datasetId the unique identifier of the dataset to retrieve
   * @return a Data Transfer Object (DTO) containing the dataset's information
   */
  public DatasetInfoDTO getDatasetInfo(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findById(Integer.valueOf(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    return getDatasetInfoDTO(datasetEntity);
  }

  /**
   * Retrieves a list of dataset information associated with a specific user ID.
   *
   * @param userId the ID of the user whose datasets are to be retrieved
   * @return a list of DatasetInfoDTO objects representing the details of the datasets
   */
  public List<DatasetInfoDTO> getDatasetInfoByUserId(String userId) {
    ArrayList<DatasetInfoDTO> datasetInfoDTOS = new ArrayList<>();
    List<DatasetEntity> userDatasets = datasetRepository.findAllByCreatedById(userId);
    for (DatasetEntity datasetEntity : userDatasets) {
      datasetInfoDTOS.add(getDatasetInfoDTO(datasetEntity));
    }
    return datasetInfoDTOS;
  }

  private DatasetInfoDTO getDatasetInfoDTO(DatasetEntity datasetEntity) {
    String datasetId = String.valueOf(datasetEntity.getDatasetId());
    Optional<TransformXsltEntity> transformXsltEntity = transformXsltRepository.findByDatasetId(datasetId);
    HarvestParametersEntity harvestParametersEntity = harvestParameterService.getDatasetHarvestingParameters(datasetId)
                                                                             .orElseThrow();
    AbstractHarvestParametersDTO abstractHarvestParametersDTO = HarvestParametersConverter.convertToHarvestParametersDTO(
        harvestParametersEntity);
    return DatasetInfoDTO.builder()
                         .datasetId(datasetId)
                         .datasetName(datasetEntity.getDatasetName())
                         .createdById(datasetEntity.getCreatedById())
                         .creationDate(datasetEntity.getCreatedDate())
                         .language(datasetEntity.getLanguage())
                         .country(datasetEntity.getCountry())
                         .abstractHarvestParametersDTO(abstractHarvestParametersDTO)
                         .transformedToEdmExternal(transformXsltEntity.isPresent())
                         .build();
  }

  /**
   * Retrieves the progress of the dataset workflow execution.
   *
   * <p>This method gathers the necessary information from various sources such as dataset entities,
   * workflow steps, and error details to compute the overall progress of the dataset workflow execution.
   *
   * @param datasetId the unique identifier of the dataset whose progress needs to be retrieved
   * @return an ExecutionProgressInfoDTO containing detailed progress information of the dataset execution workflow
   */
  @Transactional(readOnly = true)
  public ExecutionProgressInfoDTO getProgress(String datasetId) {
    DatasetEntity datasetEntity = datasetRepository.findByDatasetId(Integer.parseInt(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));
    TransformXsltEntity transformXsltEntity = transformXsltRepository.findByDatasetId(datasetId).orElse(null);

    List<FullBatchJobType> workflowSteps = WorkflowHelper.getWorkflow(datasetEntity, transformXsltEntity);
    List<ExecutionProgressByStepDTO> executionProgressByStepDTOS = new LinkedList<>();
    Long previousTotalRecords = null;
    boolean previousCompleted = true;
    for (FullBatchJobType step : workflowSteps) {
      StepStatistics stepStatistics = getStepStatistics(datasetId, step);
      List<ErrorInfoDTO> errorInfoDTOList = getErrorInfo(datasetId, step);

      long currentTotalRecords = stepStatistics.totalSuccess + stepStatistics.totalFail;

      long totalRecords = 0;
      if (previousTotalRecords == null) {
        // First step
        totalRecords = currentTotalRecords;
      } else if (previousCompleted) {
        // Use the previous step's total if it was completed
        totalRecords = previousTotalRecords;
      }
      //todo: reassess counters https://europeana.atlassian.net/browse/MET-6804
      //Due to the current frontend display
      long successWithoutDuplicatesCount = stepStatistics.totalSuccess - stepStatistics.totalDuplicates;
      long successCount = successWithoutDuplicatesCount - stepStatistics.totalDistinctWarning;
      long failCount = stepStatistics.totalFail + stepStatistics.totalDuplicates;
      long warnCount = stepStatistics.totalDistinctWarning;

      ExecutionProgressByStepDTO executionProgressByStepDto = new ExecutionProgressByStepDTO(
          step,
          totalRecords,
          successCount,
          failCount,
          warnCount,
          errorInfoDTOList
      );

      executionProgressByStepDTOS.add(executionProgressByStepDto);

      previousTotalRecords = successWithoutDuplicatesCount;
      previousCompleted = (currentTotalRecords == totalRecords);
    }

    final long totalRecords = executionProgressByStepDTOS.getFirst().total();
    final long completedRecords = executionProgressByStepDTOS.getLast().success() + executionProgressByStepDTOS.getLast().warn();
    final long totalFailInWorkflow = executionProgressByStepDTOS.stream().mapToLong(ExecutionProgressByStepDTO::fail).sum();
    final long totalProcessed = completedRecords + totalFailInWorkflow;
    final boolean recordLimitExceeded = datasetEntity.isRecordLimitExceeded();
    final TiersZeroInfoDTO tiersZeroInfoDTO = prepareTiersInfo(datasetId);

    ExecutionStatus executionStatus = computeStatus(datasetEntity, totalRecords, totalProcessed, totalFailInWorkflow);
    String publishPortalUrl = getPublishPortalUrl(datasetEntity, executionStatus);
    List<DatasetErrorInfoDTO> datasetErrorInfoDTOS = datasetEntity.getDatasetErrors().stream().map(
                                                                      datasetError -> new DatasetErrorInfoDTO(datasetError.getMessage(), Status.FAIL))
                                                                  .toList();

    return new ExecutionProgressInfoDTO(
        publishPortalUrl,
        executionStatus,
        totalRecords,
        totalProcessed,
        executionProgressByStepDTOS,
        recordLimitExceeded,
        datasetErrorInfoDTOS,
        tiersZeroInfoDTO
    );
  }

  public SandboxTaskProgress getProgressForStep(String executionId, String datasetId, FullBatchJobType step) {
    ExecutionRun executionRun = executionRunRepository.findDistinctByExecutionId(executionId);
    if (executionRun == null) {
      throw new IllegalArgumentException("ExecutionRun not found for executionId: " + executionId);
    }
    String sourceExecutionId = executionRun.getSourceExecutionId();
    long expectedRecords;
    if (sourceExecutionId == null && step.getBatchJobGroup() == HARVEST) {
      expectedRecords = executionRecordExternalIdentifierRepository.countByExecutionRun_ExecutionId(executionId);
    } else {
      expectedRecords = executionRecordRepository.countByExecutionRun_ExecutionId(sourceExecutionId);
    }
    StepStatistics stepStatistics = getStepStatistics(datasetId, step);

    long processedRecords = stepStatistics.totalSuccess + stepStatistics.totalFail;
    long successRecords = stepStatistics.totalSuccess - stepStatistics.totalDuplicates;
    long failedRecords = stepStatistics.totalFail;
    long warningRecords = stepStatistics.totalDistinctWarning;
    long deletedRecords = 0;
    long duplicatedRecords = stepStatistics.totalDuplicates;

    JobExecution jobExecution = batchJobExecutor.findJobExecutionByParameter(executionId, step);
    BatchStatus batchStatus = Optional.ofNullable(jobExecution).map(JobExecution::getStatus).orElse(BatchStatus.UNKNOWN);
    SandboxTaskState sandboxTaskState = SandboxTaskState.fromBatchStatus(batchStatus);
    return new SandboxTaskProgress(
        expectedRecords,
        processedRecords,
        successRecords,
        failedRecords,
        warningRecords,
        deletedRecords,
        duplicatedRecords,
        sandboxTaskState
    );
  }

  private ExecutionStatus computeStatus(DatasetEntity datasetEntity, long totalRecords, long totalProcessed,
      long totalFailInWorkflow) {
    ExecutionStatus executionStatus;
    if (!datasetEntity.getDatasetErrors().isEmpty()) {
      executionStatus = ExecutionStatus.FAILED;
    } else if (totalRecords > 0 && totalRecords == totalFailInWorkflow) {
      executionStatus = ExecutionStatus.FAILED;
    } else if (totalRecords == 0L) {
      executionStatus = ExecutionStatus.HARVESTING_IDENTIFIERS;
    } else if (totalRecords == totalProcessed) {
      executionStatus = ExecutionStatus.COMPLETED;
    } else {
      executionStatus = ExecutionStatus.IN_PROGRESS;
    }
    return executionStatus;
  }

  private @NotNull StepStatistics getStepStatistics(
      String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    long totalSuccess =
        executionRecordRepository.countByExecutionRun_DatasetIdAndExecutionRun_ExecutionName(datasetId, executionName);
    long totalDuplicates =
        executionRecordRepository.countDuplicateRecords(datasetId, executionName);
    long totalFailure =
        executionRecordErrorRepository.countByExecutionRun_DatasetIdAndExecutionRun_ExecutionName(datasetId, executionName);
    long totalDistinctWarning =
        executionRecordWarningRepository.countDistinctRecordIds(datasetId, executionName);
    return new StepStatistics(totalSuccess, totalDuplicates, totalFailure, totalDistinctWarning);
  }

  private List<ErrorInfoDTO> getErrorInfo(String datasetId, FullBatchJobType fullBatchJobType) {
    Map<GroupedIssueKey, List<String>> groupedIssues = collectGroupedIssues(datasetId, fullBatchJobType);

    List<ErrorInfoDTO> result = new ArrayList<>();
    for (Map.Entry<GroupedIssueKey, List<String>> entry : groupedIssues.entrySet()) {
      List<String> sortedRecordIds = entry.getValue().stream().sorted().toList();
      result.add(new ErrorInfoDTO(entry.getKey().message(), entry.getKey().status(), sortedRecordIds
      ));
    }

    return result;
  }

  private @NotNull Map<GroupedIssueKey, List<String>> collectGroupedIssues(String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    Map<GroupedIssueKey, List<String>> groupedIssues = new LinkedHashMap<>();
    try (Stream<ExecutionRecordErrorProjection> executionRecordErrors =
        executionRecordErrorRepository.findErrorsWithIdentifiers(datasetId, executionName)) {
      executionRecordErrors.forEach(executionRecordError -> {
        String recordId = formatRecordId(executionRecordError.getIdentifier());
        GroupedIssueKey key = new GroupedIssueKey(Status.FAIL, executionRecordError.getException());
        groupedIssues.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
      });
    }

    try (Stream<ExecutionRecordWarningProjection> executionRecordWarnings =
        executionRecordWarningRepository.findWarningsWithIdentifiers(datasetId, executionName)) {
      executionRecordWarnings.forEach(executionRecordWarning -> {
        String recordId = formatRecordId(executionRecordWarning.getIdentifier());
        GroupedIssueKey key = new GroupedIssueKey(Status.WARN, executionRecordWarning.getException());
        groupedIssues.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
      });
    }

    try (Stream<ExecutionRecordIdentifierProjection> stream =
        executionRecordRepository.findDuplicateRecords(datasetId, executionName)) {
      stream.forEach(duplicate -> {
        String recordId = formatRecordId(duplicate.getIdentifier());
        GroupedIssueKey key = new GroupedIssueKey(Status.FAIL, "Duplicate record detected");
        groupedIssues.computeIfAbsent(key, k -> new ArrayList<>()).add(recordId);
      });
    }

    return groupedIssues;
  }

  private static @NotNull String formatRecordId(ExecutionRecordIdentifier executionRecordIdentifier) {
    return String.format("%s | %s | %s", executionRecordIdentifier.getExternalRecordId(),
        executionRecordIdentifier.getSourceRecordId(), executionRecordIdentifier.getRecordId());
  }

  private String getPublishPortalUrl(DatasetEntity datasetEntity, ExecutionStatus executionStatus) {
    if (ExecutionStatus.HARVESTING_IDENTIFIERS == executionStatus) {
      return HARVESTING_IDENTIFIERS_MESSAGE;
    }

    if (ExecutionStatus.IN_PROGRESS == executionStatus || ExecutionStatus.FAILED == executionStatus) {
      return PROCESSING_DATASET_MESSAGE;
    }

    String datasetId = datasetEntity.getDatasetId() + SEPARATOR + datasetEntity.getDatasetName() + SUFFIX;
    return portalPublishDatasetUrl + URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
  }

  private TiersZeroInfoDTO prepareTiersInfo(String datasetId) {
    // get a list of records with content tier 0
    List<String> listOfRecordsIdsWithContentZero =
        executionRecordTierContextRepository.findTop10ByExecutionRun_DatasetIdAndContentTier(datasetId, MediaTier.T0.toString())
                                            .stream()
                                            .map(ExecutionRecordTierContext::getIdentifier)
                                            .map(ExecutionRecordIdentifier::getRecordId).toList();

    // get list of records with metadata tier 0
    List<String> listOfRecordsIdsWithMetadataZero =
        executionRecordTierContextRepository.findTop10ByExecutionRun_DatasetIdAndMetadataTier(datasetId,
                                                MetadataTier.T0.toString())
                                            .stream()
                                            .map(ExecutionRecordTierContext::getIdentifier)
                                            .map(ExecutionRecordIdentifier::getRecordId)
                                            .toList();

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatisticsDTO contentTierInfo = listOfRecordsIdsWithContentZero.isEmpty() ? null :
        new TierStatisticsDTO(
            Math.toIntExact(executionRecordTierContextRepository.countByExecutionRun_DatasetIdAndContentTier(datasetId,
                MediaTier.T0.toString())), listOfRecordsIdsWithContentZero);

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatisticsDTO metadataTierInfo = listOfRecordsIdsWithMetadataZero.isEmpty() ? null :
        new TierStatisticsDTO(
            Math.toIntExact(executionRecordTierContextRepository.countByExecutionRun_DatasetIdAndMetadataTier(datasetId,
                MetadataTier.T0.toString())),
            listOfRecordsIdsWithMetadataZero);

    // encapsulate values into TiersZeroInfo
    return contentTierInfo == null && metadataTierInfo == null ? null :
        new TiersZeroInfoDTO(contentTierInfo, metadataTierInfo);
  }

  private record StepStatistics(long totalSuccess, long totalDuplicates, long totalFail, long totalDistinctWarning) {

  }

  private record GroupedIssueKey(Status status, String message) {

  }
}
