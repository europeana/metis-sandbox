package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.dto.debias.DeBiasReportDto;


/**
 * The interface DeBias stateful.
 */
public interface DeBiasStateful {

  /**
   * Fail.
   *
   * @param datasetId the dataset id
   */
  void fail(Integer datasetId);

  /**
   * Success.
   *
   * @param datasetId the dataset id
   */
  void success(Integer datasetId);

  /**
   * Process boolean.
   *
   * @param datasetId the dataset id
   * @return the boolean
   */
  boolean process(Integer datasetId);

  /**
   * Gets state.
   *
   * @return the state
   */
  Stateful getState();

  /**
   * Sets state.
   *
   * @param state the state
   */
  void setState(Stateful state);

  /**
   * Gets ready.
   *
   * @return the ready
   */
  Stateful getReady();

  /**
   * Gets processing.
   *
   * @return the processing
   */
  Stateful getProcessing();

  /**
   * Gets completed.
   *
   * @return the completed
   */
  Stateful getCompleted();

  /**
   * Gets error.
   *
   * @return the error
   */
  Stateful getError();


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
}
