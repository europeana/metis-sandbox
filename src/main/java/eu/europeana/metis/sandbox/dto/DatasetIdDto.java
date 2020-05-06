package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import io.swagger.annotations.ApiModel;

@ApiModel("Dataset")
public class DatasetIdDto {

  private final String datasetId;

  public DatasetIdDto(String datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    this.datasetId = datasetId;
  }

  public String getDatasetId() {
    return this.datasetId;
  }
}
