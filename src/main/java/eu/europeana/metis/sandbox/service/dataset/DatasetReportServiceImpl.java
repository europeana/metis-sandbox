package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.batch.common.BatchJobSubType;
import eu.europeana.metis.sandbox.batch.common.BatchJobType;
import eu.europeana.metis.sandbox.batch.common.ValidationBatchBatchJobSubType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordException;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordIdentifier;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordWarningException;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExceptionLogRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordExternalIdentifierRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordTierContextRepository;
import eu.europeana.metis.sandbox.batch.repository.ExecutionRecordWarningExceptionRepository;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.config.batch.EnrichmentJobConfig;
import eu.europeana.metis.sandbox.config.batch.IndexingJobConfig;
import eu.europeana.metis.sandbox.config.batch.MediaJobConfig;
import eu.europeana.metis.sandbox.config.batch.NormalizationJobConfig;
import eu.europeana.metis.sandbox.config.batch.OaiHarvestJobConfig;
import eu.europeana.metis.sandbox.config.batch.TransformationJobConfig;
import eu.europeana.metis.sandbox.config.batch.ValidationJobConfig;
import eu.europeana.metis.sandbox.dto.report.DatasetLogDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogViewImpl;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private static final String EMPTY_DATASET_MESSAGE = "Dataset is empty.";
  private static final String HARVESTING_IDENTIFIERS_MESSAGE = "Harvesting dataset identifiers and records.";
  private static final String PROCESSING_DATASET_MESSAGE = "A review URL will be generated when the dataset has finished processing.";
  private static final String SEPARATOR = "_";
  private static final String SUFFIX = "*";
  private final DatasetRepository datasetRepository;
  private final DatasetLogService datasetLogService;
  private final RecordLogRepository recordLogRepository;
  private final RecordErrorLogRepository errorLogRepository;
  private final RecordRepository recordRepository;
  @Value("${sandbox.portal.publish.dataset-base-url}")
  private String portalPublishDatasetUrl;

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  private record StepConfig(BatchJobType batchJob, BatchJobSubType subtype, Step step) {

  }

  private static final List<StepConfig> stepConfigs = List.of(
      new StepConfig(OaiHarvestJobConfig.BATCH_JOB, null, Step.HARVEST_OAI_PMH),
      new StepConfig(ValidationJobConfig.BATCH_JOB, ValidationBatchBatchJobSubType.EXTERNAL, Step.VALIDATE_EXTERNAL),
      new StepConfig(TransformationJobConfig.BATCH_JOB, null, Step.TRANSFORM),
      new StepConfig(ValidationJobConfig.BATCH_JOB, ValidationBatchBatchJobSubType.INTERNAL, Step.VALIDATE_INTERNAL),
      new StepConfig(NormalizationJobConfig.BATCH_JOB, null, Step.NORMALIZE),
      new StepConfig(EnrichmentJobConfig.BATCH_JOB, null, Step.ENRICH),
      new StepConfig(MediaJobConfig.BATCH_JOB, null, Step.MEDIA_PROCESS),
      new StepConfig(IndexingJobConfig.BATCH_JOB, null, Step.PUBLISH)
  );

  public DatasetReportServiceImpl(
      DatasetRepository datasetRepository,
      DatasetLogService datasetLogService,
      RecordLogRepository recordLogRepository,
      RecordErrorLogRepository errorLogRepository,
      RecordRepository recordRepository, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.datasetRepository = datasetRepository;
    this.datasetLogService = datasetLogService;
    this.recordLogRepository = recordLogRepository;
    this.errorLogRepository = errorLogRepository;
    this.recordRepository = recordRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordWarningExceptionRepository = executionRecordWarningExceptionRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  public ProgressInfoDto getProgress(String datasetId) {
    List<StepStatistic> stepStatistics = new ArrayList<>();
    List<ErrorLogView> errorLogViews = new ArrayList<>();
    Map<Step, StepStatisticsWrapper> statisticsMap = new EnumMap<>(Step.class);

    for (StepConfig stepConfig : stepConfigs) {
      StepStatisticsWrapper stepStatisticsWrapper = getStatistics(datasetId, stepConfig);
      stepStatistics.addAll(stepStatisticsWrapper.stepStatistics());
      statisticsMap.put(stepConfig.step(), stepStatisticsWrapper);

      errorLogViews.addAll(getErrors(datasetId, stepConfig));
    }

    final DatasetEntity dataset = getDataset(datasetId);

    if (stepStatistics.isEmpty() || stepStatistics.stream().allMatch(step -> step.getStatus().equals(Status.FAIL))) {
      return new ProgressInfoDto(getPublishPortalUrl(dataset, 0L),
          dataset.getRecordsQuantity(), 0L, List.of(),
          dataset.getRecordLimitExceeded(), "All records failed to be processed. ",
          null, null);
    }

    final long completedRecords = statisticsMap.get(stepConfigs.getLast().step).totalSuccess;

    // get records processed by step
    Map<Step, Map<Status, Long>> recordsProcessedByStep = getStatisticsByStep(stepStatistics);

    // get errors by step
    Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep = getRecordErrorsByStep(errorLogViews);

    // collect steps processing information
    List<ProgressByStepDto> progressByStepDtos = new LinkedList<>();
    recordsProcessedByStep.forEach((step, statusMap) -> addStepInfo(progressByStepDtos, statusMap, step,
        recordErrorsByStep));

    TiersZeroInfo tiersZeroInfo = prepareTiersInfoNew(datasetId);

    return new ProgressInfoDto(
        getPublishPortalUrl(dataset, completedRecords),
        dataset.getRecordsQuantity(), completedRecords,
        progressByStepDtos, dataset.getRecordLimitExceeded(), getErrorMessage(dataset.getRecordsQuantity()),
        null, tiersZeroInfo);
  }

  private StepStatisticsWrapper getStatistics(String datasetId, StepConfig stepConfig) {
    return getStepStatistics(datasetId, stepConfig.batchJob(), stepConfig.subtype(), stepConfig.step());
  }

  private List<ErrorLogView> getErrors(String datasetId, StepConfig config) {
    return getErrorView(datasetId, config.batchJob(), config.subtype(), config.step());
  }

  private @NotNull DatasetReportServiceImpl.StepStatisticsWrapper getStepStatistics(
      String datasetId, BatchJobType batchJobType, BatchJobSubType batchJobSubType, Step step) {
    String executionName = getFullExecutionName(batchJobType, batchJobSubType);
    long totalSuccess = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, executionName);
    long totalFailure = executionRecordExceptionLogRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        executionName);
    long totalWarning = executionRecordWarningExceptionRepository.countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(datasetId,
        executionName);
    final long totalProcessed = totalSuccess + totalFailure;

    List<StepStatistic> stepStatistics = new ArrayList<>();
    stepStatistics.add(new StepStatistic(step, Status.SUCCESS, totalSuccess));
    stepStatistics.add(new StepStatistic(step, Status.FAIL, totalFailure));
    stepStatistics.add(new StepStatistic(step, Status.WARN, totalWarning));
    return new StepStatisticsWrapper(totalSuccess, totalProcessed, stepStatistics);
  }

  private static @NotNull String getFullExecutionName(BatchJobType batchJobType, BatchJobSubType batchJobSubType) {
    return (batchJobSubType == null) ? batchJobType.name() : (batchJobType.name() + "-" + batchJobSubType.getName());
  }

  private record StepStatisticsWrapper(long totalSuccess, long totalProcessed, List<StepStatistic> stepStatistics) {

  }

  private List<ErrorLogView> getErrorView(String datasetId, BatchJobType batchJobType, BatchJobSubType batchJobSubType,
      Step step) {
    String executionName = getFullExecutionName(batchJobType, batchJobSubType);

    List<ExecutionRecordException> recordExceptionLogs = executionRecordExceptionLogRepository.findByIdentifier_DatasetIdAndIdentifier_ExecutionName(
        datasetId, executionName);

    List<ErrorLogView> errorLogViews = new ArrayList<>();
    for (ExecutionRecordException recordExceptionLog : recordExceptionLogs) {
      RecordEntity recordEntity = new RecordEntity();
      recordEntity.setDatasetId(datasetId);
      recordEntity.setProviderId(recordExceptionLog.getIdentifier().getRecordId());
      errorLogViews.add(new ErrorLogViewImpl(null, recordEntity, step, Status.FAIL, recordExceptionLog.getException()));
    }

    List<ExecutionRecordWarningException> warningExceptions =
        executionRecordWarningExceptionRepository.findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(datasetId, executionName);

    for (ExecutionRecordWarningException warningException : warningExceptions) {
      RecordEntity recordEntity = new RecordEntity();
      recordEntity.setDatasetId(datasetId);
      recordEntity.setProviderId(warningException.getExecutionRecord().getIdentifier().getRecordId());
      errorLogViews.add(new ErrorLogViewImpl(null, recordEntity, step, Status.WARN, warningException.getMessage()));
    }
    return errorLogViews;
  }

  private static Stream<DatasetLogDto> getErrors(List<DatasetLogDto> datasetLogs) {
    return datasetLogs.stream().filter(log -> log.getType() == Status.FAIL);
  }

  private static String createMessageRecordError(RecordEntity recordEntity) {
    return Stream.of(recordEntity.getEuropeanaId(), recordEntity.getProviderId()).filter(
        Objects::nonNull).filter(id -> !id.isBlank()).collect(Collectors.joining(" | "));
  }

  @Override
  @Transactional(readOnly = true)
  public ProgressInfoDto getReport(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    // pull records and errors data for the dataset
    List<StepStatistic> stepStatistics;
    List<ErrorLogView> errorsLog;
    try {
      stepStatistics = recordLogRepository.getStepStatistics(datasetId);
      errorsLog = errorLogRepository.getByRecordIdDatasetId(datasetId);
    } catch (RuntimeException exception) {
      throw new ServiceException(format("Failed to get report for dataset id: [%s]. ", datasetId),
          exception);
    }

    // get qty of records completely processed
    final long completedRecords = getCompletedRecords(stepStatistics);

    // search for dataset
    final DatasetEntity dataset = getDataset(datasetId);

    List<DatasetLogDto> datasetLogs = datasetLogService.getAllLogs(datasetId);
    if (stepStatistics.isEmpty() || stepStatistics.stream().allMatch(step -> step.getStatus().equals(Status.FAIL))
        || getErrors(datasetLogs).findAny().isPresent()) {
      return new ProgressInfoDto(getPublishPortalUrl(dataset, 0L),
          dataset.getRecordsQuantity(), 0L, List.of(),
          dataset.getRecordLimitExceeded(), getErrorMessage(datasetLogs, dataset.getRecordsQuantity()),
          datasetLogs,
          null);
    }

    // get records processed by step
    Map<Step, Map<Status, Long>> recordsProcessedByStep = getStatisticsByStep(
        stepStatistics);

    // get errors by step
    Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep = getRecordErrorsByStep(
        errorsLog);

    // collect steps processing information
    List<ProgressByStepDto> stepsInfo = new LinkedList<>();
    recordsProcessedByStep.forEach((step, statusMap) -> addStepInfo(stepsInfo, statusMap, step,
        recordErrorsByStep));

    TiersZeroInfo tiersZeroInfo = prepareTiersInfo(datasetId);

    return new ProgressInfoDto(
        getPublishPortalUrl(dataset, completedRecords),
        dataset.getRecordsQuantity(), completedRecords,
        stepsInfo, dataset.getRecordLimitExceeded(), getErrorMessage(datasetLogs, dataset.getRecordsQuantity()),
        datasetLogs, tiersZeroInfo);
  }

  private String getPublishPortalUrl(DatasetEntity dataset, Long completedRecords) {
    return getPortalUrl(portalPublishDatasetUrl, dataset, completedRecords);
  }

  private String getPortalUrl(String portal, DatasetEntity dataset, Long completedRecords) {
    Long recordsQty = dataset.getRecordsQuantity();
    if (recordsQty == null) {
      return HARVESTING_IDENTIFIERS_MESSAGE;
    }

    if (!recordsQty.equals(completedRecords)) {
      return PROCESSING_DATASET_MESSAGE;
    }

    var datasetId = dataset.getDatasetId() + SEPARATOR + dataset.getDatasetName() + SUFFIX;
    return portal + URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
  }

  private String getErrorMessage(Long recordsQuantity) {
    if (recordsQuantity == null) {
      return "";
    } else if (recordsQuantity == 0) {
      return EMPTY_DATASET_MESSAGE;
    } else {
      return "";
    }
  }

  private String getErrorMessage(List<DatasetLogDto> datasetLogs, Long recordsQuantity) {
    if (getErrors(datasetLogs).findAny().isPresent()) {
      return getErrors(datasetLogs).map(DatasetLogDto::getMessage).collect(Collectors.joining(","));
    }

    if (recordsQuantity == null) {
      return "";
    } else if (recordsQuantity == 0) {
      return EMPTY_DATASET_MESSAGE;
    } else {
      return "";
    }
  }

  private DatasetEntity getDataset(String datasetId) {
    Optional<DatasetEntity> optionalDataset;

    try {
      optionalDataset = datasetRepository.findById(Integer.valueOf(datasetId));
    } catch (RuntimeException exception) {
      throw new ServiceException(format("Failed to get dataset with id: [%s]. ", datasetId),
          exception);
    }

    return optionalDataset.orElseThrow(() -> new InvalidDatasetException(datasetId));
  }

  private Long getCompletedRecords(List<StepStatistic> stepStatistics) {
    return stepStatistics.stream()
                         .filter(current -> current.getStep() == Step.CLOSE || current.getStatus() == Status.FAIL)
                         .mapToLong(StepStatistic::getCount)
                         .sum();
  }

  private Map<Step, Map<Status, Long>> getStatisticsByStep(
      List<StepStatistic> stepStatistics) {
    return stepStatistics.stream()
                         .filter(x -> x.getStep() != Step.CLOSE)
                         .sorted(Comparator.comparingInt(stepStatistic -> stepStatistic.getStep().precedence()))
                         .collect(groupingBy(StepStatistic::getStep, LinkedHashMap::new,
                             groupingBy(StepStatistic::getStatus,
                                 reducing(0L, StepStatistic::getCount, Long::sum))));
  }

  private Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> getRecordErrorsByStep(
      List<ErrorLogView> errorsLog) {
    if (errorsLog.isEmpty()) {
      return Map.of();
    }
    return errorsLog
        .stream()
        .sorted(Comparator.comparingInt((ErrorLogView e) -> e.getStep().precedence())
                          .thenComparing(x -> x.getRecordId().getProviderId()))
        .collect(groupingBy(ErrorLogView::getStep, LinkedHashMap::new,
            groupingBy(ErrorLogView::getStatus,
                groupingBy(ErrorLogView::getMessage))));
  }

  private void addStepInfo(List<ProgressByStepDto> stepsInfo,
      Map<Status, Long> statusMap,
      Step step,
      Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep
  ) {
    stepsInfo
        .add(new ProgressByStepDto(step,
            statusMap.getOrDefault(Status.SUCCESS, 0L),
            statusMap.getOrDefault(Status.FAIL, 0L),
            statusMap.getOrDefault(Status.WARN, 0L),
            addStepErrors(recordErrorsByStep.get(step))));
  }

  private List<ErrorInfoDto> addStepErrors(Map<Status, Map<String, List<ErrorLogView>>> statusMap) {
    if (isNull(statusMap) || statusMap.isEmpty()) {
      return List.of();
    }

    List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();

    statusMap.forEach((status, errorsMap) ->
        errorsMap.forEach((error, errorLogViews) -> errorInfoDtoList.add(
            new ErrorInfoDto(error, status,
                errorLogViews.stream()
                             .map(ErrorLogView::getRecordId)
                             .map(DatasetReportServiceImpl::createMessageRecordError)
                             .sorted(String::compareTo)
                             .toList()))));

    errorInfoDtoList.sort(Comparator.comparing(x -> x.getRecordIds().getFirst()));
    return errorInfoDtoList;
  }

  private TiersZeroInfo prepareTiersInfo(String datasetId) {
    // get list of records with content tier 0
    List<String> listOfRecordsIdsWithContentZero = recordRepository.findTop10ByDatasetIdAndContentTierOrderByEuropeanaIdAsc(
                                                                       datasetId, MediaTier.T0.toString())
                                                                   .stream().map(RecordEntity::getEuropeanaId).toList();
    // get list of records with metadata tier 0
    List<String> listOfRecordsIdsWithMetadataZero = recordRepository.findTop10ByDatasetIdAndMetadataTierOrderByEuropeanaIdAsc(
                                                                        datasetId, MetadataTier.T0.toString())
                                                                    .stream().map(RecordEntity::getEuropeanaId).toList();

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatistics contentTierInfo = listOfRecordsIdsWithContentZero.isEmpty() ? null :
        new TierStatistics(recordRepository.getRecordWithDatasetIdAndContentTierCount(datasetId, MediaTier.T0.toString()),
            listOfRecordsIdsWithContentZero);

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatistics metadataTierInfo = listOfRecordsIdsWithMetadataZero.isEmpty() ? null :
        new TierStatistics(recordRepository.getRecordWithDatasetIdAndMetadataTierCount(datasetId, MetadataTier.T0.toString()),
            listOfRecordsIdsWithMetadataZero);

    // encapsulate values into TiersZeroInfo
    return contentTierInfo == null && metadataTierInfo == null ? null :
        new TiersZeroInfo(contentTierInfo, metadataTierInfo);
  }

  private TiersZeroInfo prepareTiersInfoNew(String datasetId) {
    // get list of records with content tier 0
    List<String> listOfRecordsIdsWithContentZero =
        executionRecordTierContextRepository.findTop10ByIdentifier_DatasetIdAndContentTier(datasetId, MediaTier.T0.toString())
                                            .stream().map(ExecutionRecordTierContext::getIdentifier)
                                            .map(ExecutionRecordIdentifier::getRecordId).toList();
    // get list of records with metadata tier 0
    List<String> listOfRecordsIdsWithMetadataZero = executionRecordTierContextRepository.findTop10ByIdentifier_DatasetIdAndMetadataTier(
                                                                                            datasetId, MetadataTier.T0.toString())
                                                                                        .stream()
                                                                                        .map(
                                                                                            ExecutionRecordTierContext::getIdentifier)
                                                                                        .map(
                                                                                            ExecutionRecordIdentifier::getRecordId)
                                                                                        .toList();

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatistics contentTierInfo = listOfRecordsIdsWithContentZero.isEmpty() ? null :
        new TierStatistics(
            Math.toIntExact(executionRecordTierContextRepository.countByIdentifier_DatasetIdAndContentTier(datasetId,
                MediaTier.T0.toString())), listOfRecordsIdsWithContentZero);

    // encapsulate values into TierStatistics. Cut list of record ids into limit number
    TierStatistics metadataTierInfo = listOfRecordsIdsWithMetadataZero.isEmpty() ? null :
        new TierStatistics(
            Math.toIntExact(executionRecordTierContextRepository.countByIdentifier_DatasetIdAndMetadataTier(datasetId,
                MetadataTier.T0.toString())),
            listOfRecordsIdsWithMetadataZero);

    // encapsulate values into TiersZeroInfo
    return contentTierInfo == null && metadataTierInfo == null ? null :
        new TiersZeroInfo(contentTierInfo, metadataTierInfo);
  }
}
