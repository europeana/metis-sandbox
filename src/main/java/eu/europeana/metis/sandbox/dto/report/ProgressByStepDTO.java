package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

/**
 * Represent each step progress in the dataset report
 */
@ApiModel("ProgressByStep")
public class ProgressByStepDTO {

  private final FullBatchJobType step;
  private final long total;
  private final long success;
  private final long fail;
  private final long warn;

  @JsonInclude(Include.NON_EMPTY)
  private final List<ErrorInfoDTO> errors;

  public ProgressByStepDTO(FullBatchJobType step, long success, long fail, long warn, List<ErrorInfoDTO> errors) {
    this.step = step;
    this.total = success + fail;
    this.success = success;
    this.fail = fail;
    this.warn = warn;
    this.errors = Collections.unmodifiableList(errors);
  }

  public FullBatchJobType getStep() {
    return step;
  }

  public long getTotal() {
    return total;
  }

  public long getSuccess() {
    return success;
  }

  public long getFail() {
    return fail;
  }

  public long getWarn() {
    return warn;
  }

  public List<ErrorInfoDTO> getErrors() {
    return errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ProgressByStepDTO that)) {
      return false;
    }

    if (total != that.total) {
      return false;
    }
    if (success != that.success) {
      return false;
    }
    if (fail != that.fail) {
      return false;
    }
    if (warn != that.warn) {
      return false;
    }
    if (step != that.step) {
      return false;
    }
    return errors.equals(that.errors);
  }

  @Override
  public int hashCode() {
    int result = step.hashCode();
    result = 31 * result + (int) (total ^ (total >>> 32));
    result = 31 * result + (int) (success ^ (success >>> 32));
    result = 31 * result + (int) (fail ^ (fail >>> 32));
    result = 31 * result + (int) (warn ^ (warn >>> 32));
    result = 31 * result + errors.hashCode();
    return result;
  }
}
