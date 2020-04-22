package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.dto.DatasetInfoDto;

public interface DatasetReportService {

  /**
   * Get report on provided dataset id
   *
   * @param datasetId must not be null
   * @return dataset report
   * @throws NullPointerException if datasetId is null
   */
  DatasetInfoDto getReport(String datasetId);
}
