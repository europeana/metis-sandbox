package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.exception.InvalidDatasetException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;

public interface DatasetReportService {

  ProgressInfoDto getProgress(String datasetId);
}
