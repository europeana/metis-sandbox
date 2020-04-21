package eu.europeana.metis.sandbox.dto.report;

import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("Report")
public class ReportByStepDto {

  private Step step;

  private List<ErrorInfoDto> errors;

  public ReportByStepDto(Step step, List<ErrorInfoDto> errors) {
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
