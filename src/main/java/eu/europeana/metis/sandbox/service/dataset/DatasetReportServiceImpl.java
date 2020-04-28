package eu.europeana.metis.sandbox.service.dataset;

import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private final RecordRepository repository;

  public DatasetReportServiceImpl(
      RecordRepository repository) {
    this.repository = repository;
  }

  @Override
  public DatasetInfoDto getReport(String datasetId) {
    List<RecordEntity> report;

    try {
      report = repository
          .getByDatasetIdAndStatus(datasetId, Status.FAIL);
    } catch (RuntimeException exception) {
      throw new ServiceException("Failed getting report. Message: " + exception.getMessage(),
          exception);
    }

    Map<Step, Map<String, List<RecordEntity>>> reportGroup = report
        .stream()
        .collect(groupingBy(RecordEntity::getStep,
            groupingBy(x -> x.getRecordErrors().get(0).getMessage())));

    List<ReportByStepDto> reportList = new LinkedList<>();

    reportGroup.forEach((step, errorsMap) -> addToReportList(reportList, step, errorsMap));

    return new DatasetInfoDto("TBD", reportList);
  }

  private void addToReportList(List<ReportByStepDto> reportList, Step step,
      Map<String, List<RecordEntity>> errorsMap) {
    List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();

    errorsMap.forEach((error, recordList) -> errorInfoDtoList.add(
        new ErrorInfoDto(error, recordList.stream().map(RecordEntity::getRecordId)
            .collect(Collectors.toList()))));
    reportList.add(new ReportByStepDto(step, errorInfoDtoList));
  }
}
