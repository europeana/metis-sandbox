package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import io.swagger.annotations.ApiModel;
import java.time.LocalDateTime;

@ApiModel(ProgressInfoDto.SWAGGER_MODEL_NAME)
public class DatasetInfoDto {

  @JsonProperty("dataset-id")
  private final String datasetId;

  @JsonProperty("dataset-name")
  private final String datasetName;

  @JsonProperty("creation-date")
  private final LocalDateTime creationDate;

  @JsonProperty("language")
  private final Language language;

  @JsonProperty("country")
  private final Country country;

  public DatasetInfoDto(String datasetId, String datasetName,
      LocalDateTime creationDate,
      Language language, Country country) {
    requireNonNull(datasetId, "Dataset ID must not be null");
    requireNonNull(datasetName, "Dataset Name must not be null");
    this.creationDate = creationDate;
    this.language = language;
    this.country = country;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public LocalDateTime getCreationDate() {
    return creationDate;
  }

  public Language getLanguage() {
    return language;
  }

  public Country getCountry() {
    return country;
  }

}
