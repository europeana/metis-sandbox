package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.DatasetInfoDto;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Represents information about a dataset.
 */
public abstract class AbstractDatasetDto {

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

  protected AbstractDatasetDto(Builder<?> builder) {
    this.datasetId = builder.datasetId;
    this.datasetName = builder.datasetName;
    this.createdById = builder.createdById;
    this.creationDate = builder.creationDate;
    this.language = builder.language;
    this.country = builder.country;
  }

  /**
   * Builder class for constructing {@link DatasetInfoDto} instances.
   */
  public static abstract class Builder<B extends Builder<?>> {

    private String datasetId;
    private String datasetName;
    private String createdById;
    private ZonedDateTime creationDate;
    private Language language;
    private Country country;

    protected abstract B getThisInstance();

    /**
     * Sets the dataset ID.
     *
     * @param datasetId the dataset ID
     * @return the builder instance
     */
    public B datasetId(String datasetId) {
      this.datasetId = datasetId;
      return getThisInstance();
    }

    /**
     * Sets the dataset name.
     *
     * @param datasetName the name of the dataset
     * @return the builder instance
     */
    public B datasetName(String datasetName) {
      this.datasetName = datasetName;
      return getThisInstance();
    }

    /**
     * Sets the ID of the creator.
     *
     * @param createdById the creator's ID
     * @return the builder instance
     */
    public B createdById(String createdById) {
      this.createdById = createdById;
      return getThisInstance();
    }

    /**
     * Sets the creation date of the dataset.
     *
     * @param creationDate the creation date
     * @return the builder instance
     */
    public B creationDate(ZonedDateTime creationDate) {
      this.creationDate = creationDate;
      return getThisInstance();
    }

    /**
     * Sets the language of the dataset.
     *
     * @param language the language
     * @return the builder instance
     */
    public B language(Language language) {
      this.language = language;
      return getThisInstance();
    }

    /**
     * Sets the country of the dataset.
     *
     * @param country the country
     * @return the builder instance
     */
    public B country(Country country) {
      this.country = country;
      return getThisInstance();
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

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof AbstractDatasetDto that)) {
      return false;
    }

    return Objects.equals(datasetId, that.datasetId)
        && Objects.equals(datasetName, that.datasetName) && Objects.equals(createdById, that.createdById)
        && Objects.equals(creationDate, that.creationDate) && language == that.language && country == that.country;
  }

  @Override
  public int hashCode() {
    return Objects.hash(datasetId, datasetName, createdById, creationDate, language, country);
  }
}
