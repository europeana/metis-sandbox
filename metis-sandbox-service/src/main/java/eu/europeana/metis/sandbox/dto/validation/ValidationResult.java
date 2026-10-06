package eu.europeana.metis.sandbox.dto.validation;

import eu.europeana.metis.sandbox.common.batch.FullBatchJobType;
import java.util.List;

/**
 * Represents the result of a direct validation process for a given batch job.
 *
 * <p>This record provides details about the step of the batch job, the validation
 * messages generated, and the overall status of the validation for the record.
 */
public record ValidationResult(FullBatchJobType step,
                               List<RecordValidationMessage> messages,
                               Status status) {

  /**
   * Represents the status of an operation or process.
   */
  public enum Status {
    PASSED, FAILED
  }
}
