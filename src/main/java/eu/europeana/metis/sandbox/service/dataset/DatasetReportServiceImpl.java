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
import eu.europeana.metis.sandbox.repository.DatasetRepository;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.projection.ErrorLogView;
import eu.europeana.metis.sandbox.repository.projection.RecordLogView;
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
    List<RecordLogView> recordsLog;
    List<ErrorLogView> errorsLog;
    try {
      recordsLog = recordLogRepository.getByDatasetId(datasetId);
      errorsLog = errorLogRepository.getByDatasetId(datasetId);
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    if (recordsLog.isEmpty()) {
      return new DatasetInfoDto(dataset.getRecordsQuantity(), 0, List.of());
    }

    // get qty of records completely processed
    Integer completedRecords = getCompletedRecords(recordsLog);

    // get records processed by step
    Map<Step, Map<Status, Integer>> recordsProcessedByStep = getRecordsProcessedByStep(
        recordsLog);

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

  private Integer getCompletedRecords(List<RecordLogView> recordsLog) {
    return recordsLog.stream()
        .filter(record -> record.getStep() == Step.CLOSE || record.getStatus() == Status.FAIL)
        .map(e -> 1)
        .reduce(0, Integer::sum);
  }

  private Map<Step, Map<Status, Integer>> getRecordsProcessedByStep(
      List<RecordLogView> recordsLog) {
    return recordsLog.stream()
        .filter(x -> x.getStep() != Step.CLOSE)
        .sorted(Comparator.comparingInt(recordLogView -> recordLogView.getStep().precedence()))
        .collect(groupingBy(RecordLogView::getStep, LinkedHashMap::new,
            groupingBy(RecordLogView::getStatus, reducing(0, e -> 1, Integer::sum))));
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
      Map<Status, Integer> statusMap,
      Step step,
      Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> recordErrorsByStep
  ) {
    stepsInfo
        .add(new ProgressByStepDto(step,
            statusMap.getOrDefault(Status.SUCCESS, 0),
            statusMap.getOrDefault(Status.FAIL, 0),
            statusMap.getOrDefault(Status.WARN, 0),
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
