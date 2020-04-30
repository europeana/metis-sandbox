package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import io.swagger.annotations.ApiModel;

@ApiModel("Dataset")
public class DatasetIdDto {

  private final Integer datasetId;

  public DatasetIdDto(Integer datasetId) {
    requireNonNull(datasetId, "Dataset id must not be null");
    this.datasetId = datasetId;
  }

  public Integer getDatasetId() {
    return this.datasetId;
  }
}
