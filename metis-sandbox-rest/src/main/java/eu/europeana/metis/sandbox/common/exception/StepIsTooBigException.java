package eu.europeana.metis.sandbox.common.exception;

public class StepIsTooBigException extends ServiceException {

  public StepIsTooBigException(int availableRecords) {
    super("Step size value bigger than the dataset size: " + availableRecords);
  }
}
