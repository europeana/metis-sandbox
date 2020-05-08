package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("StepErrors")
public class StepErrorsDto {

  private Step step;

  private List<ErrorInfoDto> errors;

  public StepErrorsDto(Step step, List<ErrorInfoDto> errors) {
    requireNonNull(step, "Step must not be null");
    requireNonNull(errors, "Errors must not be null");
    this.step = step;
    this.errors = errors;
  }

  public Step getStep() {
    return step;
  }

  public void setStep(Step step) {
    this.step = step;
  }

  public List<ErrorInfoDto> getErrors() {
    return errors;
  }

  public void setErrors(List<ErrorInfoDto> errors) {
    this.errors = errors;
  }
}
