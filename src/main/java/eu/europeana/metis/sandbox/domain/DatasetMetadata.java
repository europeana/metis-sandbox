package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;

/**
 * DatasetMetadata class used in {@link eu.europeana.metis.sandbox.service.dataset.DatasetGeneratorService} to provide dataset
 * metadata to generate it.
 */
public class DatasetMetadata {
  private String datasetId;
  private String datasetName;
  private Country country;
  private Language language;

  private DatasetMetadata(Builder builder) {
    setDatasetId(builder.datasetId);
    setDatasetName(builder.datasetName);
    setCountry(builder.country);
    setLanguage(builder.language);
  }

  /**
   * New builder builder.
   *
   * @return the builder
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /**
   * Gets dataset id.
   *
   * @return the dataset id
   */
  public String getDatasetId() {
    return datasetId;
  }

  /**
   * Sets dataset id.
   *
   * @param datasetId the dataset id
   */
  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  /**
   * Gets dataset name.
   *
   * @return the dataset name
   */
  public String getDatasetName() {
    return datasetName;
  }

  /**
   * Sets dataset name.
   *
   * @param datasetName the dataset name
   */
  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  /**
   * Gets country.
   *
   * @return the country
   */
  public Country getCountry() {
    return country;
  }

  /**
   * Sets country.
   *
   * @param country the country
   */
  public void setCountry(Country country) {
    this.country = country;
  }

  /**
   * Gets language.
   *
   * @return the language
   */
  public Language getLanguage() {
    return language;
  }

  /**
   * Sets language.
   *
   * @param language the language
   */
  public void setLanguage(Language language) {
    this.language = language;
  }

  /**
   * {@code DatasetMetadata} builder static inner class.
   */
  public static final class Builder {

    private String datasetId;
    private String datasetName;
    private Country country;
    private Language language;

    private Builder() {
    }

    /**
     * Sets the {@code datasetId} and returns a reference to this Builder enabling method chaining.
     *
     * @param datasetId the {@code datasetId} to set
     * @return a reference to this Builder
     */
    public Builder withDatasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /**
     * Sets the {@code datasetName} and returns a reference to this Builder enabling method chaining.
     *
     * @param datasetName the {@code datasetName} to set
     * @return a reference to this Builder
     */
    public Builder withDatasetName(String datasetName) {
      this.datasetName = datasetName;
      return this;
    }

    /**
     * Sets the {@code country} and returns a reference to this Builder enabling method chaining.
     *
     * @param country the {@code country} to set
     * @return a reference to this Builder
     */
    public Builder withCountry(Country country) {
      this.country = country;
      return this;
    }

    /**
     * Sets the {@code language} and returns a reference to this Builder enabling method chaining.
     *
     * @param language the {@code language} to set
     * @return a reference to this Builder
     */
    public Builder withLanguage(Language language) {
      this.language = language;
      return this;
    }

    /**
     * Returns a {@code DatasetMetadata} built from the parameters previously set.
     *
     * @return a {@code DatasetMetadata} built with parameters of this {@code DatasetMetadata.Builder}
     */
    public DatasetMetadata build() {
      return new DatasetMetadata(this);
    }
  }
}
