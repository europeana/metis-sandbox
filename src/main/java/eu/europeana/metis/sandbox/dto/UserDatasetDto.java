package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;
import io.swagger.annotations.ApiModel;
import java.util.Objects;

/**
 * Represents information about a user dataset.
 */
@ApiModel(UserDatasetDto.SWAGGER_MODEL_NAME)
public final class UserDatasetDto extends DatasetDto {

  public static final String SWAGGER_MODEL_NAME = "UserDataset";

  @JsonProperty("harvest-protocol")
  private final HarvestProtocol harvestProtocol;

  @JsonProperty("status")
  private final Status status;

  @JsonProperty("total-records")
  private final Long totalRecords;

  @JsonProperty("processed-records")
  private final Long processedRecords;

  /**
   * constructor / default initialisation
   */
  private UserDatasetDto(Builder builder) {
    super(builder);
    this.harvestProtocol = builder.harvestProtocol;
    this.status = builder.status;
    this.totalRecords = builder.totalRecords;
    this.processedRecords = builder.processedRecords;
  }

  /**
   * Builder class for constructing {@link DatasetInfoDto} instances.
   */
  public static class Builder extends AbstractBuilder<Builder> {

    private HarvestProtocol harvestProtocol;
    private Status status;
    private Long totalRecords;
    private Long processedRecords;

    @Override
    protected Builder getThisInstance() {
      return this;
    }

    /**
     * Sets the protocol through which the dataset was uploaded.
     *
     * @param harvestProtocol the protocol through which the dataset was uploaded
     * @return the builder instance
     */
    public Builder harvestProtocol(HarvestProtocol harvestProtocol) {
      this.harvestProtocol = harvestProtocol;
      return this;
    }

    /**
     * Sets the status of the dataset.
     *
     * @param status the status of the dataset
     * @return the builder instance
     */
    public Builder status(Status status) {
      this.status = status;
      return this;
    }

    /**
     * Sets the total count of records.
     *
     * @param totalRecords the total count of records
     * @return the builder instance
     */
    public Builder totalRecords(Long totalRecords) {
      this.totalRecords = totalRecords;
      return this;
    }

    /**
     * Sets the count of processed records.
     *
     * @param processedRecords the count of processed records
     * @return the builder instance
     */
    public Builder processedRecords(Long processedRecords) {
      this.processedRecords = processedRecords;
      return this;
    }

    /**
     * Builds the {@link DatasetInfoDto} instance.
     *
     * @return the constructed DatasetInfoDto
     */
    public UserDatasetDto build() {
      return new UserDatasetDto(this);
    }
  }

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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    UserDatasetDto that = (UserDatasetDto) o;
    return harvestProtocol == that.harvestProtocol && status == that.status
        && Objects.equals(totalRecords, that.totalRecords) && Objects.equals(
        processedRecords, that.processedRecords);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), harvestProtocol, status, totalRecords, processedRecords);
  }
}
