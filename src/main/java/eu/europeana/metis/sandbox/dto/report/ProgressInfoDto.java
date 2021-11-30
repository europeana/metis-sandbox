package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

/**
 * Base of the dataset report
 */
@ApiModel(ProgressInfoDto.PROGRESS_SWAGGER_MODEL_NAME)
public class ProgressInfoDto {

  public static final String PROGRESS_SWAGGER_MODEL_NAME = "ProgressInfo";

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

  @JsonProperty("dataset-info")
  private final DatasetInfoDto datasetInfoDto;

  public ProgressInfoDto(String portalPreviewUrl, String portalPublishUrl,
      int totalRecords,
      long processedRecords, List<ProgressByStepDto> progressByStep, DatasetInfoDto datasetInfoDto) {
    this.totalRecords = totalRecords;
    this.processedRecords = processedRecords;
    this.status =
        this.totalRecords == this.processedRecords ? Status.COMPLETED : Status.IN_PROGRESS;
    this.progressByStep = Collections.unmodifiableList(progressByStep);
    this.portalPreviewUrl = portalPreviewUrl;
    this.portalPublishUrl = portalPublishUrl;
    this.datasetInfoDto = datasetInfoDto;
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

  public DatasetInfoDto getDatasetInfoDto() {
    return datasetInfoDto;
  }
}
