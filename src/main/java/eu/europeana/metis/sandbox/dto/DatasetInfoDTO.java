package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;


/**
 * Represents information about a dataset.
 */
@ApiModel(DatasetInfoDTO.SWAGGER_MODEL_NAME)
public final class DatasetInfoDTO {

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
  private final HarvestingParametersDTO harvestingParametersDto;

  private DatasetInfoDTO(Builder builder) {
    this.datasetId = builder.datasetId;
    this.datasetName = builder.datasetName;
    this.createdById = builder.createdById;
    this.creationDate = builder.creationDate;
    this.language = builder.language;
    this.country = builder.country;
    this.transformedToEdmExternal = builder.transformedToEdmExternal;
    this.harvestingParametersDto = builder.harvestingParametersDto;
  }

  /**
   * Builder class for constructing {@link DatasetInfoDTO} instances.
   */
  public static class Builder {

    private String datasetId;
    private String datasetName;
    private String createdById;
    private ZonedDateTime creationDate;
    private Language language;
    private Country country;
    private boolean transformedToEdmExternal;
    private HarvestingParametersDTO harvestingParametersDto;

    /**
     * Sets the dataset ID.
     *
     * @param datasetId the dataset ID
     * @return the builder instance
     */
    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /**
     * Sets the dataset name.
     *
     * @param datasetName the name of the dataset
     * @return the builder instance
     */
    public Builder datasetName(String datasetName) {
      this.datasetName = datasetName;
      return this;
    }

    /**
     * Sets the ID of the creator.
     *
     * @param createdById the creator's ID
     * @return the builder instance
     */
    public Builder createdById(String createdById) {
      this.createdById = createdById;
      return this;
    }

    /**
     * Sets the creation date of the dataset.
     *
     * @param creationDate the creation date
     * @return the builder instance
     */
    public Builder creationDate(ZonedDateTime creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    /**
     * Sets the language of the dataset.
     *
     * @param language the language
     * @return the builder instance
     */
    public Builder language(Language language) {
      this.language = language;
      return this;
    }

    /**
     * Sets the country of the dataset.
     *
     * @param country the country
     * @return the builder instance
     */
    public Builder country(Country country) {
      this.country = country;
      return this;
    }

    /**
     * Sets whether the dataset was transformed to EDM for external use.
     *
     * @param transformedToEdmExternal true if transformed, false otherwise
     * @return the builder instance
     */
    public Builder transformedToEdmExternal(boolean transformedToEdmExternal) {
      this.transformedToEdmExternal = transformedToEdmExternal;
      return this;
    }

    /**
     * Sets the harvesting parameters for the dataset.
     *
     * @param harvestingParametersDto the harvesting parameters
     * @return the builder instance
     */
    public Builder harvestingParametricDto(HarvestingParametersDTO harvestingParametersDto) {
      this.harvestingParametersDto = harvestingParametersDto;
      return this;
    }

    /**
     * Builds the {@link DatasetInfoDTO} instance.
     *
     * @return the constructed DatasetInfoDto
     */
    public DatasetInfoDTO build() {
      return new DatasetInfoDTO(this);
    }
  }

  /**
   * Gets the dataset ID.
   *
   * @return the dataset ID
   */
  public String getDatasetId() {
    return datasetId;
  }

  /**
   * Gets the dataset name.
   *
   * @return the dataset name
   */
  public String getDatasetName() {
    return datasetName;
  }

  /**
   * Gets the ID of the creator.
   *
   * @return the creator ID
   */
  public String getCreatedById() {
    return createdById;
  }

  /**
   * Gets the creation date.
   *
   * @return the creation date
   */
  public ZonedDateTime getCreationDate() {
    return creationDate;
  }

  /**
   * Gets the language of the dataset.
   *
   * @return the language
   */
  public Language getLanguage() {
    return language;
  }

  /**
   * Gets the country of the dataset.
   *
   * @return the country
   */
  public Country getCountry() {
    return country;
  }

  /**
   * Indicates whether the dataset was transformed to EDM for external use.
   *
   * @return true if transformed, false otherwise
   */
  public boolean isTransformedToEdmExternal() {
    return transformedToEdmExternal;
  }

  /**
   * Gets the harvesting parameters.
   *
   * @return the harvesting parameters
   */
  public HarvestingParametersDTO getHarvestingParametricDto() {
    return harvestingParametersDto;
  }
}
