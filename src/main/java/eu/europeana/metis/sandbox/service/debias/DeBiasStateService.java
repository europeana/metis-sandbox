package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;
import eu.europeana.metis.sandbox.dto.debias.DeBiasStatusDto;


/**
 * The interface DeBias stateful.
 */
public interface DeBiasStateService {

  /**
   * Process boolean.
   *
   * @param datasetId the dataset id
   * @return the boolean
   */
  boolean process(Integer datasetId);

  /**
   * Gets DeBias report.
   *
   * @param datasetId the dataset id
   * @return the de bias report
   */
  DeBiasReportDto getDeBiasReport(Integer datasetId);

  /**
   * Clean DeBias report.
   *
   * @param datasetId the dataset id
   */
  void cleanDeBiasReport(Integer datasetId);

  /**
   * Gets DeBias status.
   *
   * @param datasetId the dataset id
   * @return the de bias status
   */
  DeBiasStatusDto getDeBiasStatus(Integer datasetId);
}
