package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

@ApiModel("ProgressByStep")
public class ProgressByStepDto {

  @JsonProperty("step-info")
  private final List<StepInfoDto> stepInfo;

  public ProgressByStepDto(List<StepInfoDto> stepInfo) {
    this.stepInfo = Collections.unmodifiableList(stepInfo);
  }

  public List<StepInfoDto> getStepInfo() {
    return stepInfo;
  }
}
