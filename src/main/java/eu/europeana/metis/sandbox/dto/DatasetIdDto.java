package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.domain.Dataset;
import io.swagger.annotations.ApiModel;

@ApiModel(DatasetIdDto.SWAGGER_MODEL_NAME)
public class DatasetIdDto {

  public static final String SWAGGER_MODEL_NAME = "Dataset";

  @JsonProperty(value = "dataset-id")
  private final String datasetId;
  @JsonProperty(value = "records-to-process")
  private final int recordsToProcess;
  @JsonProperty(value = "duplicate-records")
  private final int duplicateRecords;

  public DatasetIdDto(Dataset dataset) {
    requireNonNull(dataset, "Dataset id must not be null");
    this.recordsToProcess = dataset.getRecords().size();
    this.duplicateRecords = dataset.getDuplicates();
    this.datasetId = dataset.getDatasetId();
  }

  public String getDatasetId() {
    return this.datasetId;
  }

  public int getRecordsToProcess() {
    return recordsToProcess;
  }

  public int getDuplicateRecords() {
    return duplicateRecords;
  }
}
