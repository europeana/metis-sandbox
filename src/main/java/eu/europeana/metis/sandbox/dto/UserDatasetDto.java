package eu.europeana.metis.sandbox.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.common.Status;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;


/**
 * Represents information about a user dataset.
 */
@ApiModel(UserDatasetDto.SWAGGER_MODEL_NAME)
public final class UserDatasetDto {

  public static final String SWAGGER_MODEL_NAME = "UserDataset";

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

  //@JsonProperty("transformed-to-edm-external")
  //private final boolean transformedToEdmExternal;

  //@JsonProperty("harvesting-parameters")
  //private final HarvestingParametricDto harvestingParametricDto;

  @JsonProperty("harvest-protocol")
  private final HarvestProtocol harvestProtocol;

  @JsonProperty("status")
  private final Status status;

  @JsonProperty("total-records")
  private final Long totalRecords;

  @JsonProperty("processed-records")
  private final Long processedRecords;

  private UserDatasetDto(Builder builder) {
    this.datasetId = builder.datasetId;
    this.datasetName = builder.datasetName;
    this.createdById = builder.createdById;
    this.creationDate = builder.creationDate;
    this.language = builder.language;
    this.country = builder.country;
    //this.transformedToEdmExternal = builder.transformedToEdmExternal;
    //this.harvestingParametricDto = builder.harvestingParametricDto;
    this.harvestProtocol = builder.harvestProtocol;
    this.status = builder.status;
    this.totalRecords = builder.totalRecords;
    this.processedRecords = builder.processedRecords;
  }

  /**
   * Builder class for constructing {@link UserDatasetDto} instances.
   */
  public static class Builder {

    private String datasetId;
    private String datasetName;
    private String createdById;
    private ZonedDateTime creationDate;
    private Language language;
    private Country country;
    //private boolean transformedToEdmExternal;
    //private HarvestingParametricDto harvestingParametricDto;
    private HarvestProtocol harvestProtocol;
    private Status status;
    private Long totalRecords;// = 0L;
    private Long processedRecords;// = 0L;

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
    public Builder transformedToEdmExternal(boolean transformedToEdmExternal) {
      this.transformedToEdmExternal = transformedToEdmExternal;
      return this;
    }
    */

    /**
     * Sets the harvesting parameters for the dataset.
     *
     * @param harvestingParametricDto the harvesting parameters
     * @return the builder instance
    public Builder harvestingParametricDto(HarvestingParametricDto harvestingParametricDto) {
      this.harvestingParametricDto = harvestingParametricDto;
      return this;
    }
    */

    /**
     * Sets the harvest protocol for the dataset.
     *
     * @param HarvestProtocol the harvest protocol
     * @return the builder instance
     */
    public Builder harvestProtocol(HarvestProtocol harvestProtocol) {
      this.harvestProtocol = harvestProtocol;
      return this;
    }

    /**
     * Sets the status for the dataset.
     *
     * @param Status the status
     * @return the builder instance
     */
    public Builder status(Status status) {
      this.status = status;
      return this;
    }

    public Builder totalRecords(Long totalRecords) {
      this.totalRecords = totalRecords;
      return this;
    }

    public Builder processedRecords(Long processedRecords) {
      this.processedRecords = processedRecords;
      return this;
    }

    /**
     * Builds the {@link UserDatasetDto} instance.
     *
     * @return the constructed UserDatasetDto
     */
    public UserDatasetDto build() {
      return new UserDatasetDto(this);
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
  public boolean isTransformedToEdmExternal() {
    return transformedToEdmExternal;
  }
  */

  /**
   * Gets the harvesting parameters.
   *
   * @return the harvesting parameters
  public HarvestingParametricDto getHarvestingParametricDto() {
    return harvestingParametricDto;
  }
  */

  /**
   * Gets the harvest protocol
   *
   * @return the harvesting parameters
   */
  public HarvestProtocol getHarvestProtocol() {
    return harvestProtocol;
  }

  /**
   * Gets the harvest protocol
   *
   * @return the harvesting parameters
   */
  public Status getStatus() {
    return status;
  }

  public long getTotalRecords() {
    return totalRecords;
  }

  public long getProcessedRecords() {
    return processedRecords;
  }

}
