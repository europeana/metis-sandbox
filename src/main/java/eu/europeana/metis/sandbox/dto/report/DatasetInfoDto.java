package eu.europeana.metis.sandbox.dto.report;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

/**
 * Base of the dataset report
 */
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

  @JsonProperty("portal-preview")
  private final String portalPreviewUrl;

  @JsonProperty("portal-publish")
  private final String portalPublishUrl;

  private final Status status;

  @JsonProperty("total-records")
  private final long totalRecords;

  @JsonProperty("processed-records")
  private final long processedRecords;

  @JsonProperty("progress-by-step")
  private final List<ProgressByStepDto> progressByStep;

  public DatasetInfoDto(String portalPreviewUrl, String portalPublishUrl,
      int totalRecords,
      long processedRecords, List<ProgressByStepDto> progressByStep) {
    requireNonNull(portalPreviewUrl, "Preview portal url must not be null");
    requireNonNull(portalPublishUrl, "Publish portal url must not be null");
    requireNonNull(progressByStep, "Progress by step must not be null");
    this.totalRecords = totalRecords;
    this.processedRecords = processedRecords;
    this.status =
        this.totalRecords == this.processedRecords ? Status.COMPLETED : Status.IN_PROGRESS;
    this.progressByStep = Collections.unmodifiableList(progressByStep);
    this.portalPreviewUrl = portalPreviewUrl;
    this.portalPublishUrl = portalPublishUrl;
  }

  public String getPortalPreviewUrl() {
    return portalPreviewUrl;
  }

  public String getPortalPublishUrl() {
    return portalPublishUrl;
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
