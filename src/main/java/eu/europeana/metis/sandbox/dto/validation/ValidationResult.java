package eu.europeana.metis.sandbox.dto.validation;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import eu.europeana.metis.sandbox.controller.ValidationController;
import java.util.List;

/**
 * Represents the result of a direct validation process for a given batch job.
 *
 * <p>This record provides details about the step of the batch job, the validation
 * messages generated, and the overall status of the validation for the record.
 * <p>Note: this is specifically used for {@link ValidationController}
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
