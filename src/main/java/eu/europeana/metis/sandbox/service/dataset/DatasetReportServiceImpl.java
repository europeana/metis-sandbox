package eu.europeana.metis.sandbox.service.dataset;

import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.repository.RecordErrorLogRepository;
import eu.europeana.metis.sandbox.repository.projection.DatasetReportView;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private final RecordErrorLogRepository repository;

  public DatasetReportServiceImpl(
      RecordErrorLogRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public DatasetInfoDto getReport(String datasetId) {
    List<DatasetReportView> report;

    try {
      report = repository.getByDatasetId(datasetId);
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    Map<Step, Map<String, List<DatasetReportView>>> reportGroup = report
        .stream()
        .collect(groupingBy(DatasetReportView::getStep,
            groupingBy(DatasetReportView::getMessage)));

    List<ReportByStepDto> reportList = new LinkedList<>();

    reportGroup.forEach((step, errorsMap) -> addToReportList(reportList, step, errorsMap));

    return new DatasetInfoDto("TBD", reportList);
  }

  private void addToReportList(List<ReportByStepDto> reportList, Step step,
      Map<String, List<DatasetReportView>> errorsMap) {
    List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();

    errorsMap.forEach((error, recordList) -> errorInfoDtoList.add(
        new ErrorInfoDto(error, recordList.stream().map(DatasetReportView::getRecordId)
            .collect(Collectors.toList()))));
    reportList.add(new ReportByStepDto(step, errorInfoDtoList));
  }
}
