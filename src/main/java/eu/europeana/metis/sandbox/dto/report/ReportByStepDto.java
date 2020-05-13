package eu.europeana.metis.sandbox.dto.report;

import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

@ApiModel("Report")
public class ReportByStepDto {

  private final Step step;

  private final List<ErrorInfoDto> errors;

  public ReportByStepDto(Step step, List<ErrorInfoDto> errors) {
    this.step = step;
    this.errors = Collections.unmodifiableList(errors);
  }

  public Step getStep() {
    return step;
  }

  public List<ErrorInfoDto> getErrors() {
    return errors;
  }
}
