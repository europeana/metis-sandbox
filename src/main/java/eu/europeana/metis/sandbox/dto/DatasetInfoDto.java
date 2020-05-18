package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import eu.europeana.metis.sandbox.dto.report.ProgressByStepDto;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

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
  private final Long totalRecords;

  @JsonProperty("processed-records")
  private final Long processedRecords;

  @JsonProperty("progress-by-step")
  private final List<ProgressByStepDto> progressByStep;

  public DatasetInfoDto(Integer totalRecords,
      Long processedRecords, List<ProgressByStepDto> progressByStep) {
    requireNonNull(totalRecords, "Total records must not be null");
    requireNonNull(processedRecords, "Processed records must not be null");
    requireNonNull(progressByStep, "Progress by step must not be null");
    this.totalRecords = Long.valueOf(totalRecords);
    this.status =
        this.totalRecords.equals(processedRecords) ? Status.COMPLETED : Status.IN_PROGRESS;
    this.processedRecords = processedRecords;
    this.progressByStep = Collections.unmodifiableList(progressByStep);
  }

  public Status getStatus() {
    return status;
  }

  public Long getTotalRecords() {
    return totalRecords;
  }

  public Long getProcessedRecords() {
    return processedRecords;
  }

  public List<ProgressByStepDto> getProgressByStep() {
    return progressByStep;
  }
}
