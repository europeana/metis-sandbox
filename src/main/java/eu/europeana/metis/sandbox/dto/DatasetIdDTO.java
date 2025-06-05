package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@ApiModel(DatasetIdDTO.SWAGGER_MODEL_NAME)
@Getter
@AllArgsConstructor
public class DatasetIdDTO {

  public static final String SWAGGER_MODEL_NAME = "Dataset";

  @JsonProperty(value = "dataset-id")
  private final String datasetId;
}
