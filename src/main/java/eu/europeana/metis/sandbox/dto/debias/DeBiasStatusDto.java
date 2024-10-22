package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;

/**
 * The type Detection info dto.
 */
@ApiModel(DeBiasStatusDto.SWAGGER_MODEL_NAME)
public class DeBiasStatusDto {

  /**
   * The constant SWAGGER_MODEL_NAME.
   */
  public static final String SWAGGER_MODEL_NAME = "DeBiasStatusDto";

  @JsonProperty("dataset-id")
  private final Integer datasetId;

  @JsonProperty("state")
  private final String state;

  @JsonProperty("creation-date")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private final ZonedDateTime creationDate;

  @JsonProperty("total")
  private final Integer total;

  @JsonProperty("processed")
  private final Integer processed;

  /**
   * Instantiates a new Detection info dto.
   *
   * @param datasetId the dataset id
   * @param state the state
   * @param creationDate the creation date
   * @param total the total
   * @param processed the proccessed
   */
  public DeBiasStatusDto(Integer datasetId, String state, ZonedDateTime creationDate,
      Integer total, Integer processed) {
    this.datasetId = datasetId;
    this.state = state;
    this.creationDate = creationDate;
    this.total = total;
    this.processed = processed;
  }

  /**
   * Gets dataset id.
   *
   * @return the dataset id
   */
  public Integer getDatasetId() {
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

  /**
   * Gets total.
   *
   * @return the total
   */
  public Integer getTotal() {
    return total;
  }

  /**
   * Gets success.
   *
   * @return the success
   */
  public Integer getProcessed() {
    return processed;
  }
}
