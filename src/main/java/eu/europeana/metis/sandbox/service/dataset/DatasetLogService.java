package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.dto.report.DatasetLogDto;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface DatasetLogService {

  @Transactional
  Void log(String datasetId, Status status, String message, Throwable exception);

  @Transactional
  Void logException(String datasetId, Throwable exception);

  List<DatasetLogDto> getAllLogs(String datasetId);
}