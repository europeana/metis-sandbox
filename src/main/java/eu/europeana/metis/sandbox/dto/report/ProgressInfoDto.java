package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel("ProgressInfo")
public class ProgressInfoDto {

  @JsonProperty("total-records")
  private final Integer totalRecords;

  @JsonProperty("processed")
  private final Integer processedRecords;

  @JsonProperty("progress-by-step")
  private final ProgressByStepDto progressByStep;

  public ProgressInfoDto(Integer totalRecords,
      Integer processedRecords, ProgressByStepDto progressByStep) {
    this.totalRecords = totalRecords;
    this.processedRecords = processedRecords;
    this.progressByStep = progressByStep;
  }

  public Integer getTotalRecords() {
    return totalRecords;
  }

  public Integer getProcessedRecords() {
    return processedRecords;
  }

  public ProgressByStepDto getProgressByStep() {
    return progressByStep;
  }
}
