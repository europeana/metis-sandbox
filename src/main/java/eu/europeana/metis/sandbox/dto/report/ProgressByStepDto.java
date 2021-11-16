package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

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
}
