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

  private final String portalUrl;

  private final Status status;

  @JsonProperty("total-records")
  private final long totalRecords;

  @JsonProperty("processed-records")
  private final long processedRecords;

  @JsonProperty("progress-by-step")
  private final List<ProgressByStepDto> progressByStep;

  public DatasetInfoDto(String portalUrl, int totalRecords,
      long processedRecords, List<ProgressByStepDto> progressByStep) {
    requireNonNull(portalUrl, "Portal url must not be null");
    requireNonNull(progressByStep, "Progress by step must not be null");
    this.totalRecords = totalRecords;
    this.processedRecords = processedRecords;
    this.status =
        this.totalRecords == this.processedRecords ? Status.COMPLETED : Status.IN_PROGRESS;
    this.progressByStep = Collections.unmodifiableList(progressByStep);
    this.portalUrl = portalUrl;
  }

  public String getPortalUrl() {
    return portalUrl;
  }

  public Status getStatus() {
    return status;
  }

  public long getTotalRecords() {
    return totalRecords;
  }

  public long getProcessedRecords() {
    return processedRecords;
  }

  public List<ProgressByStepDto> getProgressByStep() {
    return progressByStep;
  }
}
