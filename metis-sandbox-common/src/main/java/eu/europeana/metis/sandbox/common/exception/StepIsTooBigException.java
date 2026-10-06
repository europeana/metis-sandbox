package eu.europeana.metis.sandbox.common.exception;

/**
 * Exception indicating that the step size requested is larger than the available dataset size.
 */
public class StepIsTooBigException extends ServiceException {

  /**
   * Constructs a new StepIsTooBigException with a detail message indicating the step size value exceeds the available dataset
   * size.
   *
   * @param availableRecords the number of available records in the dataset.
   */
  public StepIsTooBigException(int availableRecords) {
    super("Step size value bigger than the dataset size: " + availableRecords);
  }
}
