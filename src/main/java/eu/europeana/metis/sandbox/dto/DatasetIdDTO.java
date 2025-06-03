package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(DatasetIdDTO.SWAGGER_MODEL_NAME)
public class DatasetIdDTO {

  public static final String SWAGGER_MODEL_NAME = "Dataset";

  @JsonProperty(value = "dataset-id")
  private final String datasetId;


  public DatasetIdDTO(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetId() {
    return this.datasetId;
  }

}
