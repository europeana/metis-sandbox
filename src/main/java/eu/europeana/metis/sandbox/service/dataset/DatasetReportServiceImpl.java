package eu.europeana.metis.sandbox.service.dataset;

import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import eu.europeana.metis.sandbox.repository.projection.DatasetReportView;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private final RecordLogRepository repository;

  public DatasetReportServiceImpl(
      RecordLogRepository repository) {
    this.repository = repository;
  }

  @Override
  public DatasetInfoDto getReport(String datasetId) {
    var report = repository.getByKeyDatasetIdAndResult(datasetId, Status.FAIL);
    Map<Step, Map<String, List<DatasetReportView>>> reportGroup = report
        .stream()
        .collect(groupingBy(x -> x.getKey().getStep(),
            groupingBy(DatasetReportView::getError)));

    List<ReportByStepDto> reportList = new LinkedList<>();
    reportGroup.forEach((step, map) -> {
      List<ErrorInfoDto> errorInfoDtoList = new LinkedList<>();
      map.forEach((error, recordList) -> errorInfoDtoList.add(
          new ErrorInfoDto(error, recordList.stream().map(record -> record.getKey().getId())
              .collect(Collectors.toList()))));
      reportList.add(new ReportByStepDto(step, errorInfoDtoList));
    });

    return new DatasetInfoDto("TBD", reportList);
  }
}
