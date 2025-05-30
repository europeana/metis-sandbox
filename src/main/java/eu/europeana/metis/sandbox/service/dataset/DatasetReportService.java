package eu.europeana.metis.sandbox.service.dataset;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
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
import eu.europeana.metis.sandbox.common.aggregation.StepStatistic;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.TierStatistics;
import eu.europeana.metis.sandbox.dto.report.TiersZeroInfo;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogView;
import eu.europeana.metis.sandbox.entity.projection.ErrorLogViewImpl;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.service.engine.WorkflowHelper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DatasetReportService {

  private static final String EMPTY_DATASET_MESSAGE = "Dataset is empty.";
  private static final String HARVESTING_IDENTIFIERS_MESSAGE = "Harvesting dataset identifiers and records.";
  private static final String PROCESSING_DATASET_MESSAGE = "A review URL will be generated when the dataset has finished processing.";
  private static final String SEPARATOR = "_";
  private static final String SUFFIX = "*";
  private final DatasetRepository datasetRepository;
  @Value("${sandbox.portal.publish.dataset-base-url}")
  private String portalPublishDatasetUrl;

  private final ExecutionRecordRepository executionRecordRepository;
  private final ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository;
  private final ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository;
  private final ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository;
  private final ExecutionRecordTierContextRepository executionRecordTierContextRepository;

  public DatasetReportService(
      DatasetRepository datasetRepository, ExecutionRecordRepository executionRecordRepository,
      ExecutionRecordExceptionLogRepository executionRecordExceptionLogRepository,
      ExecutionRecordWarningExceptionRepository executionRecordWarningExceptionRepository,
      ExecutionRecordExternalIdentifierRepository executionRecordExternalIdentifierRepository,
      ExecutionRecordTierContextRepository executionRecordTierContextRepository) {
    this.datasetRepository = datasetRepository;
    this.executionRecordRepository = executionRecordRepository;
    this.executionRecordExceptionLogRepository = executionRecordExceptionLogRepository;
    this.executionRecordWarningExceptionRepository = executionRecordWarningExceptionRepository;
    this.executionRecordExternalIdentifierRepository = executionRecordExternalIdentifierRepository;
    this.executionRecordTierContextRepository = executionRecordTierContextRepository;
  }

  public ProgressInfoDto getProgress(String datasetId) {
    List<StepStatistic> stepStatistics = new ArrayList<>();
    List<ErrorLogView> errorLogViews = new ArrayList<>();
    Map<FullBatchJobType, StepStatisticsWrapper> statisticsMap = new EnumMap<>(FullBatchJobType.class);

    DatasetEntity datasetEntity = datasetRepository.findByDatasetId(Integer.parseInt(datasetId))
                                                   .orElseThrow(() -> new InvalidDatasetException(datasetId));

    List<FullBatchJobType> workflowSteps = WorkflowHelper.getWorkflow(datasetEntity);

    for (FullBatchJobType step : workflowSteps) {
      StepStatisticsWrapper stepStatisticsWrapper = getStatistics(datasetId, step);
      stepStatistics.addAll(stepStatisticsWrapper.stepStatistics());
      statisticsMap.put(step, stepStatisticsWrapper);

      errorLogViews.addAll(getErrors(datasetId, step));
    }

    final DatasetEntity dataset = getDataset(datasetId);

    if (stepStatistics.isEmpty() || stepStatistics.stream().allMatch(step -> step.getStatus().equals(Status.FAIL))) {
      return new ProgressInfoDto(getPublishPortalUrl(dataset, 0L),
          dataset.getRecordsQuantity(), 0L, List.of(),
          dataset.getRecordLimitExceeded(), "All records failed to be processed. ",
          null, null);
    }

    final long completedRecords = statisticsMap.get(workflowSteps.getLast()).totalSuccess;

    // get records processed by step
    Map<FullBatchJobType, Map<Status, Long>> recordsProcessedByStep = getStatisticsByStep(stepStatistics);

    // get errors by step
    Map<FullBatchJobType, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep = getRecordErrorsByStep(errorLogViews);

    // collect steps processing information
    List<ProgressByStepDto> progressByStepDtos = new LinkedList<>();
    recordsProcessedByStep.forEach((step, statusMap) -> addStepInfo(progressByStepDtos, statusMap, step,
        recordErrorsByStep));
    long totalFailInWorkflow = progressByStepDtos.stream().mapToLong(ProgressByStepDto::getFail).sum();

    TiersZeroInfo tiersZeroInfo = prepareTiersInfo(datasetId);

    return new ProgressInfoDto(
        getPublishPortalUrl(dataset, completedRecords),
        dataset.getRecordsQuantity(), completedRecords + totalFailInWorkflow,
        progressByStepDtos, dataset.getRecordLimitExceeded(), getErrorMessage(dataset.getRecordsQuantity()),
        null, tiersZeroInfo);
  }

  private StepStatisticsWrapper getStatistics(String datasetId, FullBatchJobType fullBatchJobType) {
    return getStepStatistics(datasetId, fullBatchJobType);
  }

  private List<ErrorLogView> getErrors(String datasetId, FullBatchJobType fullBatchJobType) {
    return getErrorView(datasetId, fullBatchJobType);
  }

  private @NotNull DatasetReportService.StepStatisticsWrapper getStepStatistics(
      String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    long totalSuccess = executionRecordRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        executionName);
    long totalFailure = executionRecordExceptionLogRepository.countByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId,
        executionName);
    long totalWarning = executionRecordWarningExceptionRepository.countByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
        datasetId,
        executionName);
    final long totalProcessed = totalSuccess + totalFailure;

    List<StepStatistic> stepStatistics = new ArrayList<>();
    stepStatistics.add(new StepStatistic(fullBatchJobType, Status.SUCCESS, totalSuccess));
    stepStatistics.add(new StepStatistic(fullBatchJobType, Status.FAIL, totalFailure));
    stepStatistics.add(new StepStatistic(fullBatchJobType, Status.WARN, totalWarning));
    return new StepStatisticsWrapper(totalSuccess, totalProcessed, stepStatistics);
  }

  private record StepStatisticsWrapper(long totalSuccess, long totalProcessed, List<StepStatistic> stepStatistics) {

  }

  private List<ErrorLogView> getErrorView(String datasetId, FullBatchJobType fullBatchJobType) {
    String executionName = fullBatchJobType.name();
    List<ExecutionRecordException> recordExceptionLogs =
        executionRecordExceptionLogRepository.findByIdentifier_DatasetIdAndIdentifier_ExecutionName(datasetId, executionName);

    List<ErrorLogView> errorLogViews = new ArrayList<>();
    for (ExecutionRecordException recordExceptionLog : recordExceptionLogs) {
      errorLogViews.add(new ErrorLogViewImpl(null, recordExceptionLog.getIdentifier().getRecordId(), fullBatchJobType, Status.FAIL,
          recordExceptionLog.getException()));
    }

    List<ExecutionRecordWarningException> warningExceptions =
        executionRecordWarningExceptionRepository.findByExecutionRecord_Identifier_DatasetIdAndExecutionRecord_Identifier_ExecutionName(
            datasetId, executionName);

    for (ExecutionRecordWarningException warningException : warningExceptions) {
      errorLogViews.add(
          new ErrorLogViewImpl(null, warningException.getExecutionRecord().getIdentifier().getRecordId(), fullBatchJobType, Status.WARN,
              warningException.getMessage()));
    }
    return errorLogViews;
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

  private Map<FullBatchJobType, Map<Status, Long>> getStatisticsByStep(
      List<StepStatistic> stepStatistics) {
    return stepStatistics.stream()
                         .sorted(Comparator.comparing(StepStatistic::getStep))
                         .collect(groupingBy(StepStatistic::getStep, LinkedHashMap::new,
                             groupingBy(StepStatistic::getStatus,
                                 reducing(0L, StepStatistic::getCount, Long::sum))));
  }

  private Map<FullBatchJobType, Map<Status, Map<String, List<ErrorLogView>>>> getRecordErrorsByStep(
      List<ErrorLogView> errorsLog) {
    if (errorsLog.isEmpty()) {
      return Map.of();
    }
    return errorsLog
        .stream()
        .sorted(Comparator.comparing(ErrorLogView::getStep).thenComparing(ErrorLogView::getRecordId))
        .collect(groupingBy(ErrorLogView::getStep, LinkedHashMap::new,
            groupingBy(ErrorLogView::getStatus,
                groupingBy(ErrorLogView::getMessage))));
  }

  private void addStepInfo(List<ProgressByStepDto> stepsInfo,
      Map<Status, Long> statusMap,
      FullBatchJobType step,
      Map<FullBatchJobType, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep
  ) {
    stepsInfo.add(new ProgressByStepDto(step,
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
                             .sorted(String::compareTo)
                             .toList()))));

    errorInfoDtoList.sort(Comparator.comparing(x -> x.getRecordIds().getFirst()));
    return errorInfoDtoList;
  }

  private TiersZeroInfo prepareTiersInfo(String datasetId) {
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
