package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

@ApiModel("ProgressInfo")
public class ProgressInfoDto {

  private enum Status {
    COMPLETED("completed"),
    IN_PROGRESS("in progress");

    private final String value;

    Status(String value) {
      this.value = value;
    }

    @JsonValue
    public String value() {
      return value;
    }
  }

  private final Status status;

  @JsonProperty("total-records")
  private final Integer totalRecords;

  @JsonProperty("processed-records")
  private final Integer processedRecords;

  @JsonProperty("progress-by-step")
  private final List<ProgressByStepDto> progressByStep;

  public ProgressInfoDto(Integer totalRecords,
      Integer processedRecords, List<ProgressByStepDto> progressByStep) {
    requireNonNull(totalRecords, "Total records must not be null");
    requireNonNull(processedRecords, "Processed records must not be null");
    requireNonNull(progressByStep, "Progress by step must not be null");
    this.status = totalRecords.equals(processedRecords) ? Status.COMPLETED : Status.IN_PROGRESS;
    this.totalRecords = totalRecords;
    this.processedRecords = processedRecords;
    this.progressByStep = Collections.unmodifiableList(progressByStep);
  }

  public Status getStatus() {
    return status;
  }

  public Integer getTotalRecords() {
    return totalRecords;
  }

  public Integer getProcessedRecords() {
    return processedRecords;
  }

  public List<ProgressByStepDto> getProgressByStep() {
    return progressByStep;
  }
}
