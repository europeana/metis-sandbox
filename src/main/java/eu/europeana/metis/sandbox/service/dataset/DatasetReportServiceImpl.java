package eu.europeana.metis.sandbox.service.dataset;

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
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.StepErrorsDto;
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

    DatasetEntity dataset = getDatasetEntity(datasetId);

    List<RecordLogView> recordsLog;
    List<ErrorLogView> errorsLog;
    try {
      recordsLog = recordLogRepository.getByDatasetId(datasetId);
      errorsLog = errorLogRepository.getByDatasetId(datasetId);
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    Integer completedRecords = recordsLog.stream()
        .filter(this::hasFinishedOrFailed)
        .map(e -> 1)
        .reduce(0, Integer::sum);

    ProgressInfoDto progressInfo;
    if (!recordsLog.isEmpty()) {
      Map<Step, Map<Status, Integer>> processedByStepRecords = recordsLog.stream()
          .sorted(Comparator.comparingInt(recordLogView -> recordLogView.getStep().precedence()))
          .collect(groupingBy(RecordLogView::getStep, LinkedHashMap::new,
              groupingBy(RecordLogView::getStatus, reducing(0, e -> 1, Integer::sum))));
      List<ProgressByStepDto> stepsInfo = new LinkedList<>();
      processedByStepRecords.forEach((step, statusMap) -> {
        stepsInfo
            .add(new ProgressByStepDto(step,
                statusMap.getOrDefault(Status.SUCCESS, 0),
                statusMap.getOrDefault(Status.FAIL, 0),
                statusMap.getOrDefault(Status.WARN, 0)));
      });
      progressInfo = new ProgressInfoDto(dataset.getRecordsQuantity(), completedRecords, stepsInfo);
    } else {
      progressInfo = new ProgressInfoDto(dataset.getRecordsQuantity(), completedRecords, List.of());
    }

    List<StepErrorsDto> reportList = new LinkedList<>();
    if (!errorsLog.isEmpty()) {
      Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> reportGroup = errorsLog
          .stream()
          .sorted(Comparator.comparingInt((ErrorLogView e) -> e.getStep().precedence())
          .thenComparing(ErrorLogView::getRecordId))
          .collect(groupingBy(ErrorLogView::getStep, LinkedHashMap::new,
              groupingBy(ErrorLogView::getStatus,
                  groupingBy(ErrorLogView::getMessage))));

      reportGroup.forEach((step, statusMap) -> addToReportList(reportList, step, statusMap));
    }

    return new DatasetInfoDto(progressInfo, reportList);
  }

  private boolean hasFinishedOrFailed(RecordLogView record) {
    return record.getStep() == Step.INDEX || record.getStatus() == Status.FAIL;
  }

  private DatasetEntity getDatasetEntity(String datasetId) {
    Optional<DatasetEntity> optionalDataset;

    try {
      optionalDataset = datasetRepository.findById(Integer.valueOf(datasetId));
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    return optionalDataset.orElseThrow(() -> new InvalidDatasetException(datasetId));
  }

  private void addToReportList(List<StepErrorsDto> reportList, Step step,
      Map<Status, Map<String, List<ErrorLogView>>> statusMap) {
    List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();

    statusMap.forEach((status, errorsMap) ->
        errorsMap.forEach((error, recordList) -> errorInfoDtoList.add(
            new ErrorInfoDto(error, status, recordList.stream()
                .map(ErrorLogView::getRecordId)
                .sorted(String::compareTo)
                .collect(toList())))));
    reportList.add(new StepErrorsDto(step, errorInfoDtoList));
  }
}
