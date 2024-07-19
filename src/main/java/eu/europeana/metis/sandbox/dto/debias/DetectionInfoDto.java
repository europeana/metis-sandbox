package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;

/**
 * The type Detection info dto.
 */
@ApiModel(DetectionInfoDto.SWAGGER_MODEL_NAME)
public class DetectionInfoDto {

  /**
   * The constant SWAGGER_MODEL_NAME.
   */
  public static final String SWAGGER_MODEL_NAME = "DebiasDetectionInfo";

  @JsonProperty("dataset-id")
  private final Long datasetId;

  @JsonProperty("state")
  private final String state;

  @JsonProperty("creation-date")
  @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.nZ")
  private final ZonedDateTime creationDate;

  /**
   * Instantiates a new Detection info dto.
   *
   * @param datasetId the dataset id
   * @param state the state
   * @param creationDate the creation date
   */
  public DetectionInfoDto(Long datasetId, String state, ZonedDateTime creationDate) {
    this.datasetId = datasetId;
    this.state = state;
    this.creationDate = creationDate;
  }

  /**
   * Gets dataset id.
   *
   * @return the dataset id
   */
  public Long getDatasetId() {
    return datasetId;
  }

  /**
   * Gets state.
   *
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Gets creation date.
   *
   * @return the creation date
   */
  public ZonedDateTime getCreationDate() {
    return creationDate;
  }
}
