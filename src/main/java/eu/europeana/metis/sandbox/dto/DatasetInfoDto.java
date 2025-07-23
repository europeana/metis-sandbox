package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.Objects;


/**
 * Represents information about a dataset.
 */
@ApiModel(DatasetInfoDto.SWAGGER_MODEL_NAME)
public final class DatasetInfoDto extends DatasetDto {

  public static final String SWAGGER_MODEL_NAME = "DatasetInfo";

  @JsonProperty("transformed-to-edm-external")
  private final boolean transformedToEdmExternal;

  @JsonProperty("harvesting-parameters")
  private final HarvestingParametricDto harvestingParametricDto;

  private DatasetInfoDto(Builder builder) {
    super(builder);
    this.transformedToEdmExternal = builder.transformedToEdmExternal;
    this.harvestingParametricDto = builder.harvestingParametricDto;
  }

  /**
   * Builder class for constructing {@link DatasetInfoDto} instances.
   */
  public static class Builder extends AbstractBuilder<Builder> {

    private boolean transformedToEdmExternal;
    private HarvestingParametricDto harvestingParametricDto;

    @Override
    protected Builder getThisInstance() {
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
     * @param harvestingParametricDto the harvesting parameters
     * @return the builder instance
     */
    public Builder harvestingParametricDto(HarvestingParametricDto harvestingParametricDto) {
      this.harvestingParametricDto = harvestingParametricDto;
      return this;
    }

    /**
     * Builds the {@link DatasetInfoDto} instance.
     *
     * @return the constructed DatasetInfoDto
     */
    public DatasetInfoDto build() {
      return new DatasetInfoDto(this);
    }
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
  public HarvestingParametricDto getHarvestingParametricDto() {
    return harvestingParametricDto;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DatasetInfoDto that)) {
      return false;
    }

    return super.equals(o) && transformedToEdmExternal == that.transformedToEdmExternal
        && Objects.equals(harvestingParametricDto, that.harvestingParametricDto);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(),transformedToEdmExternal,harvestingParametricDto);
  }
}
