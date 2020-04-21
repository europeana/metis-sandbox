package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.dto.DatasetInfoDto;

public interface DatasetReportService {

  /**
   *
   * @param datasetId
   * @return
   */
  DatasetInfoDto getReport(String datasetId);
}
