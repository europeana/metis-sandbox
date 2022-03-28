package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.domain.Dataset;
import io.swagger.annotations.ApiModel;

@ApiModel(DatasetIdDto.SWAGGER_MODEL_NAME)
public class DatasetIdDto {

  public static final String SWAGGER_MODEL_NAME = "Dataset";

  @JsonProperty(value = "dataset-id")
  private final String datasetId;


  public DatasetIdDto(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetId() {
    return this.datasetId;
  }

}
