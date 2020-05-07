package eu.europeana.metis.sandbox.dto.report;

import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;

@ApiModel("StepInfo")
public class StepInfoDto {

  private final Step step;
  private final int total;
  private final int success;
  private final int fail;

  public StepInfoDto(Step step, int success, int fail) {
    this.step = step;
    this.total = success + fail;
    this.success = success;
    this.fail = fail;
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
}
