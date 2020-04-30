package eu.europeana.metis.sandbox.service.dataset;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.projection.ErrorLogView;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private final RecordErrorLogRepository errorLogRepository;

  public DatasetReportServiceImpl(
      RecordErrorLogRepository errorLogRepository) {
    this.errorLogRepository = errorLogRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public DatasetInfoDto getReport(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");

    List<ErrorLogView> report;
    try {
      report = errorLogRepository.getByDatasetId(datasetId);
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    if (!report.isEmpty()) {
      Map<Step, Map<Status, Map<String, List<ErrorLogView>>>> reportGroup = report
          .stream()
          .sorted(Comparator.comparing(ErrorLogView::getStep))
          .collect(groupingBy(ErrorLogView::getStep, LinkedHashMap::new,
              groupingBy(ErrorLogView::getStatus,
                  groupingBy(ErrorLogView::getMessage))));

      List<ReportByStepDto> reportList = new LinkedList<>();

      reportGroup.forEach((step, statusMap) -> addToReportList(reportList, step, statusMap));
      return new DatasetInfoDto("TBD", reportList);
    }

    return new DatasetInfoDto("TBD", List.of());
  }

  private void addToReportList(List<ReportByStepDto> reportList, Step step,
      Map<Status, Map<String, List<ErrorLogView>>> statusMap) {
    List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();

    statusMap.forEach((status, errorsMap) ->
        errorsMap.forEach((error, recordList) -> errorInfoDtoList.add(
            new ErrorInfoDto(error, status, recordList.stream().map(ErrorLogView::getRecordId)
                .collect(Collectors.toList())))));
    reportList.add(new ReportByStepDto(step, errorInfoDtoList));
  }
}
