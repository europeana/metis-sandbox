package eu.europeana.metis.sandbox.service.dataset;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.toList;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import eu.europeana.metis.sandbox.entity.DatasetEntity;
import eu.europeana.metis.sandbox.entity.StepStatistic;
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.projection.ErrorLogView;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private static final int FIRST = 0;

  private final DatasetRepository datasetRepository;
  private final RecordLogRepository recordLogRepository;
  private final RecordErrorLogRepository errorLogRepository;

  public DatasetReportServiceImpl(
      DatasetRepository datasetRepository,
      RecordLogRepository recordLogRepository,
      RecordErrorLogRepository errorLogRepository) {
    this.datasetRepository = datasetRepository;
    this.recordLogRepository = recordLogRepository;
    this.errorLogRepository = errorLogRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public DatasetInfoDto getReport(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    // search for dataset
    DatasetEntity dataset = getDataset(datasetId);

    // pull records and errors data for the dataset
    List<StepStatistic> stepStatistics;
    List<ErrorLogView> errorsLog;
    try {
      stepStatistics = recordLogRepository.getStepStatistics(datasetId);
      errorsLog = errorLogRepository.getByDatasetId(datasetId);
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    if (stepStatistics.isEmpty()) {
      return new DatasetInfoDto(dataset.getRecordsQuantity(), 0L, List.of());
    }

    // get qty of records completely processed
    Long completedRecords = getCompletedRecords(stepStatistics);

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

    return new DatasetInfoDto(dataset.getRecordsQuantity(), completedRecords, stepsInfo);
  }

  private DatasetEntity getDataset(String datasetId) {
    Optional<DatasetEntity> optionalDataset;

    try {
      optionalDataset = datasetRepository.findById(Integer.valueOf(datasetId));
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
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
            .thenComparing(ErrorLogView::getRecordId))
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
        errorsMap.forEach((error, recordList) -> errorInfoDtoList.add(
            new ErrorInfoDto(error, status, recordList.stream()
                .map(ErrorLogView::getRecordId)
                .sorted(String::compareTo)
                .collect(toList())))));

    errorInfoDtoList.sort(Comparator.comparing(x -> x.getRecordIds().get(FIRST)));
    return errorInfoDtoList;
  }
}
