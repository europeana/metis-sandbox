package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDto;
import eu.europeana.metis.sandbox.entity.debias.DatasetDeBiasEntity;


/**
 * The interface DeBias stateful.
 */
public interface DeBiasStateService {

  DeBiasReportDto getDeBiasReport(String datasetId);

  /**
   * Clean DeBias report.
   *
   * @param datasetId the dataset id
   */
  void cleanDeBiasReport(Integer datasetId);

  DeBiasStatusDto getDeBiasStatus(String datasetId);

  DatasetDeBiasEntity createDatasetDeBiasEntity(Integer datasetId);
}
