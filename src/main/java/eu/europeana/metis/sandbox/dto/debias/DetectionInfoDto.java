package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;

@ApiModel(DetectionInfoDto.SWAGGER_MODEL_NAME)
public class DetectionInfoDto {
  public static final String SWAGGER_MODEL_NAME = "DebiasDetectionInfo";

  @JsonProperty("dataset-id")
  private final String datasetId;

  @JsonProperty("state")
  private final String state;

  @JsonProperty("creation-date")
  @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.nZ")
  private final ZonedDateTime creationDate;

  public DetectionInfoDto(String datasetId, String state, ZonedDateTime creationDate) {
    this.datasetId = datasetId;
    this.state = state;
    this.creationDate = creationDate;
  }
}
