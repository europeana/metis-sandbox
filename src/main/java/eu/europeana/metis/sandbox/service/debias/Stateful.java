package eu.europeana.metis.sandbox.service.debias;

/**
 * The interface Stateful.
 */
public interface Stateful {

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
}
