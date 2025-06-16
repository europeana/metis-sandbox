package eu.europeana.metis.sandbox.batch.entity;

/**
 * Interface for accessing the identifier parts of an execution record.
 */
public interface ExecutionRecordIdAccess {

  /**
   * Retrieves the dataset identifier.
   *
   * @return The dataset identifier as a String.
   */
  String getDatasetId();

  /**
   * Retrieves the execution identifier.
   *
   * @return The execution identifier as a String.
   */
  String getExecutionId();

  /**
   * Retrieves the execution name.
   *
   * @return The execution name as a String.
   */
  String getExecutionName();

  /**
   * Retrieves the source record identifier.
   *
   * @return The source record identifier as a String.
   */
  String getSourceRecordId();
}
