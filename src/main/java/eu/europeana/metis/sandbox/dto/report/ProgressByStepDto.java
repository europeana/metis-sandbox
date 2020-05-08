package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;

@ApiModel("ProgressByStep")
public class ProgressByStepDto {

  private final Step step;
  private final int total;
  private final int success;
  private final int fail;
  private final int warn;

  public ProgressByStepDto(Step step, int success, int fail, int warn) {
    requireNonNull(step, "Step must not be null");
    this.step = step;
    this.total = success + fail + warn;
    this.success = success;
    this.fail = fail;
    this.warn = warn;
  }

  public Step getStep() {
    return step;
  }

  public int getTotal() {
    return total;
  }

  public int getSuccess() {
    return success;
  }

  public int getFail() {
    return fail;
  }

  public int getWarn() {
    return warn;
  }
}
