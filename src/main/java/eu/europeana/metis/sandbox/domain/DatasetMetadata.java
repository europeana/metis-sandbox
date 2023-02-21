package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;

/**
 * DatasetMetadata class used in {@link eu.europeana.metis.sandbox.service.dataset.DatasetGeneratorService} to provide dataset
 * metadata to generate it.
 */
public final class DatasetMetadata {
  private final String datasetId;
  private final String datasetName;
  private final Country country;
  private final Language language;
  private final Integer stepSize;

  private DatasetMetadata(DatasetMetadataBuilder datasetMetadataBuilder) {
    this.datasetId = datasetMetadataBuilder.datasetId;
    this.datasetName = datasetMetadataBuilder.datasetName;
    this.country = datasetMetadataBuilder.country;
    this.language = datasetMetadataBuilder.language;
    this.stepSize = datasetMetadataBuilder.stepSize;
  }

  /**
   * New builder builder.
   *
   * @return the builder
   */
  public static DatasetMetadataBuilder builder() {
    return new DatasetMetadataBuilder();
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
   * Gets dataset name.
   *
   * @return the dataset name
   */
  public String getDatasetName() {
    return datasetName;
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
   * Gets language.
   *
   * @return the language
   */
  public Language getLanguage() {
    return language;
  }

  /**
   * Return the value of step size
   *
   * @return the step size value as an Integer
   */
  public Integer getStepSize() {
    return stepSize;
  }

  /**
   * {@code DatasetMetadata} builder static inner class.
   */
  public static final class DatasetMetadataBuilder {

    private String datasetId;
    private String datasetName;
    private Country country;
    private Language language;
    private Integer stepSize;


    /**
     * Sets the {@code datasetId} and returns a reference to this Builder enabling method chaining.
     *
     * @param datasetId the {@code datasetId} to set
     * @return a reference to this Builder
     */
    public DatasetMetadataBuilder withDatasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /**
     * Sets the {@code datasetName} and returns a reference to this Builder enabling method chaining.
     *
     * @param datasetName the {@code datasetName} to set
     * @return a reference to this Builder
     */
    public DatasetMetadataBuilder withDatasetName(String datasetName) {
      this.datasetName = datasetName;
      return this;
    }

    /**
     * Sets the {@code country} and returns a reference to this Builder enabling method chaining.
     *
     * @param country the {@code country} to set
     * @return a reference to this Builder
     */
    public DatasetMetadataBuilder withCountry(Country country) {
      this.country = country;
      return this;
    }

    /**
     * Sets the {@code language} and returns a reference to this Builder enabling method chaining.
     *
     * @param language the {@code language} to set
     * @return a reference to this Builder
     */
    public DatasetMetadataBuilder withLanguage(Language language) {
      this.language = language;
      return this;
    }

    public DatasetMetadataBuilder withStepSize(Integer stepSize){
      this.stepSize = stepSize;
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
