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

  public enum Status {
    HARVESTING_IDENTIFIERS("harvesting identifiers"),
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

  @JsonProperty("portal-publish")
  private final String portalPublishUrl;

  private final Status status;

  @JsonProperty("total-records")
  private final int totalRecords;

  @JsonProperty("processed-records")
  private final long processedRecords;

  @JsonProperty("progress-by-step")
  private final List<ProgressByStepDto> progressByStep;

  @JsonProperty("dataset-info")
  private final DatasetInfoDto datasetInfoDto;

  public ProgressInfoDto(String portalPublishUrl, int totalRecords, long processedRecords,
      List<ProgressByStepDto> progressByStep, DatasetInfoDto datasetInfoDto) {
    
    this.processedRecords = processedRecords;
    if (totalRecords == this.processedRecords) {
      this.status = Status.COMPLETED;
      this.totalRecords = totalRecords;
    } else if (totalRecords == -1) {
      this.status = Status.HARVESTING_IDENTIFIERS;
      this.totalRecords = 0;
    } else {
      this.status = Status.IN_PROGRESS;
      this.totalRecords = totalRecords;
    }
    this.progressByStep = Collections.unmodifiableList(progressByStep);
    this.portalPublishUrl = portalPublishUrl;
    this.datasetInfoDto = datasetInfoDto;
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
