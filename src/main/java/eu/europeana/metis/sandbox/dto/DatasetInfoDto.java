package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;


/**
 * Represents information about a dataset.
 */
@ApiModel(DatasetInfoDto.SWAGGER_MODEL_NAME)
public class DatasetInfoDto {

  public static final String SWAGGER_MODEL_NAME = "DatasetInfo";

  @JsonProperty("dataset-id")
  private final String datasetId;

  @JsonProperty("dataset-name")
  private final String datasetName;

  @JsonProperty("created-by-id")
  private final String createdById;

  @JsonProperty("creation-date")
  private final ZonedDateTime creationDate;

  @JsonProperty("language")
  private final Language language;

  @JsonProperty("country")
  private final Country country;

  @JsonProperty("transformed-to-edm-external")
  private final boolean transformedToEdmExternal;

  @JsonProperty("harvesting-parameters")
  private final HarvestingParametricDto harvestingParametricDto;

  /**
   * Constructs a new DatasetInfoDto object with the provided dataset information.
   *
   * @param datasetId the identifier of the dataset.
   * @param datasetName the name of the dataset.
   * @param createdById the identifier of the creator.
   * @param creationDate the date when the dataset was created.
   * @param language the language of the dataset.
   * @param country the country of the dataset.
   * @param harvestingParametricDto parameters related to the method of harvesting data.
   * @param transformedToEdmExternal a flag indicating whether the dataset was transformed to EDM for external purposes.
   */
  public DatasetInfoDto(String datasetId, String datasetName, String createdById, ZonedDateTime creationDate,
      Language language, Country country, HarvestingParametricDto harvestingParametricDto,
      boolean transformedToEdmExternal) {

    this.creationDate = creationDate;
    this.language = language;
    this.country = country;
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.createdById = createdById;
    this.harvestingParametricDto = harvestingParametricDto;
    this.transformedToEdmExternal = transformedToEdmExternal;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public String getCreatedById() {
    return createdById;
  }

  public ZonedDateTime getCreationDate() {
    return creationDate;
  }

  public Language getLanguage() {
    return language;
  }

  public Country getCountry() {
    return country;
  }

  public HarvestingParametricDto getHarvestingParametricDto() {
    return harvestingParametricDto;
  }

  public boolean isTransformedToEdmExternal() {
    return transformedToEdmExternal;
  }
}

