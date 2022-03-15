package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represent each step progress in the dataset report
 */
@ApiModel("ProgressByStep")
public class ProgressByStepDto {

  private final Step step;
  private final long total;
  private final long success;
  private final long fail;
  private final long warn;

  @JsonInclude(Include.NON_EMPTY)
  private final List<ErrorInfoDto> errors;

  public ProgressByStepDto(Step step, long success, long fail, long warn,
      List<ErrorInfoDto> errors) {
    this.step = step;
    this.total = success + fail + warn;
    this.success = success;
    this.fail = fail;
    this.warn = warn;
    this.errors = Collections.unmodifiableList(errors);
  }

  public Step getStep() {
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

  public List<ErrorInfoDto> getErrors() {
    return errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof ProgressByStepDto)) {
      return false;
    }

    ProgressByStepDto that = (ProgressByStepDto) o;

    return new EqualsBuilder().append(total, that.total)
                              .append(success, that.success)
                              .append(fail, that.fail)
                              .append(warn, that.warn)
                              .append(step, that.step)
                              .append(errors, that.errors).isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(step)
        .append(total)
        .append(success)
        .append(fail)
        .append(warn)
        .append(errors)
        .toHashCode();
  }
}
