package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import io.swagger.annotations.ApiModel;
import java.time.LocalDateTime;

@ApiModel(DatasetInfoDto.SWAGGER_MODEL_NAME)
public class DatasetInfoDto {

  public static final String SWAGGER_MODEL_NAME = "DatasetInfo";

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

  @JsonProperty("record-limit-exceeded")
  private final boolean recordLimitExceeded;

  @JsonProperty("transformed-to-edm-external")
  private final boolean transformedToEdmExternal;

  public DatasetInfoDto(String datasetId, String datasetName,
      LocalDateTime creationDate,
      Language language, Country country, boolean recordLimitExceeded, boolean transformedToEdmExternal) {

    this.creationDate = creationDate;
    this.language = language;
    this.country = country;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.recordLimitExceeded = recordLimitExceeded;
    this.transformedToEdmExternal = transformedToEdmExternal;
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

  public boolean isRecordLimitExceeded() {
    return recordLimitExceeded;
  }
  public boolean isTransformedToEdmExternal() {
    return transformedToEdmExternal;
  }
}
