package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.debias.DebiasState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Getter;

/**
 * The type Detection info dto.
 */
@Schema(name = DeBiasStatusDTO.SWAGGER_MODEL_NAME)
@Getter
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
  private final Instant creationDate;

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
   * @param total the total records
   * @param processed the processed records
   */
  public DeBiasStatusDTO(Integer datasetId, DebiasState debiasState, Instant creationDate,
      Long total, Long processed) {
    this.datasetId = datasetId;
    this.debiasState = debiasState;
    this.creationDate = creationDate;
    this.total = total;
    this.processed = processed;
  }
}
