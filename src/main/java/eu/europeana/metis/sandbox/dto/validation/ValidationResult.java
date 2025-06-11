package eu.europeana.metis.sandbox.dto.validation;

import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import java.util.List;

/**
 * The type Validation result.
 */
public record ValidationResult(FullBatchJobType step,
                               List<RecordValidationMessage> messages,
                               Status status) {
  public enum Status {
    PASSED, FAILED
  }
}
