package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;

/**
 * The type Detection info dto.
 */
@ApiModel(DeBiasStatusDTO.SWAGGER_MODEL_NAME)
public class DeBiasStatusDTO {

  /**
   * The constant SWAGGER_MODEL_NAME.
   */
  public static final String SWAGGER_MODEL_NAME = "DeBiasStatusDto";

  @JsonProperty("dataset-id")
  private final Integer datasetId;

  @JsonProperty("state")
  private final DebiasState debiasState;

  @JsonProperty("creation-date")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private final ZonedDateTime creationDate;

  @JsonProperty("total-records")
  private final Long total;

  @JsonProperty("processed-records")
  private final Long processed;

  /**
   * Instantiates a new Detection info dto.
   *
   * @param datasetId the dataset id
   * @param debiasState the state
   * @param creationDate the creation date
   * @param total the total
   * @param processed the proccessed
   */
  public DeBiasStatusDTO(Integer datasetId, DebiasState debiasState, ZonedDateTime creationDate,
      Long total, Long processed) {
    this.datasetId = datasetId;
    this.debiasState = debiasState;
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
  public DebiasState getDebiasState() {
    return debiasState;
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
  public Long getTotal() {
    return total;
  }

  /**
   * Gets success.
   *
   * @return the success
   */
  public Long getProcessed() {
    return processed;
  }
}
