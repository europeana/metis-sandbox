package eu.europeana.metis.sandbox.service.dataset;

import static java.util.stream.Collectors.groupingBy;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import eu.europeana.metis.sandbox.dto.report.ErrorInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import eu.europeana.metis.sandbox.entity.RecordLogEntity;
import eu.europeana.metis.sandbox.repository.RecordLogRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class DatasetReportServiceImpl implements DatasetReportService {

  private RecordLogRepository repository;

  public DatasetReportServiceImpl(
      RecordLogRepository repository) {
    this.repository = repository;
  }

  @Override
  public DatasetInfoDto getReport(String datasetId) {
    var report = repository.getByKeyDatasetIdAndResult("1", Status.FAIL.name());
    Map<Step, Map<String, List<RecordLogEntity>>> reportGroup = report
        .stream()
        .collect(groupingBy(x -> x.getKey().getStep(),
            groupingBy(RecordLogEntity::getError)));

//    var reportByStep = new ReportByStepDto();
//    var datasetInfo = new DatasetInfoDto("TBD", );
    return null;
  }
}
