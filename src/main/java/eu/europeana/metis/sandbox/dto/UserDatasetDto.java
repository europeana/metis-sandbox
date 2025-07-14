package eu.europeana.metis.sandbox.dto;
//import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
//import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto.Status;

//import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;


/**
 * Represents information about a user dataset.
 */
//@ApiModel(UserDatasetDto.SWAGGER_MODEL_NAME)
public final class UserDatasetDto {

  //public static final String SWAGGER_MODEL_NAME = "UserDataset";

  //@JsonProperty("dataset-id")
  private String datasetId;

  //@JsonProperty("dataset-name")
  private String datasetName;

  //@JsonProperty("created-by-id")
  private String createdById;

  //@JsonProperty("creation-date")
  private ZonedDateTime creationDate;

  //@JsonProperty("language")
  private Language language;

  //@JsonProperty("country")
  private Country country;

  //@JsonProperty("harvest-protocol")
  private HarvestProtocol harvestProtocol;

  //@JsonProperty("status")
  private Status status;

  //@JsonProperty("total-records")
  private Long totalRecords;

  //@JsonProperty("processed-records")
  private Long processedRecords;

  public UserDatasetDto() {
    this.datasetId = "";
    this.datasetName = "";
    this.createdById = "";
    this.creationDate = ZonedDateTime.now();
    this.language = Language.EN;
    this.country = Country.NETHERLANDS;
    this.harvestProtocol = HarvestProtocol.FILE;
    this.status = Status.FAILED;
    this.totalRecords = 0L;
    this.processedRecords = 0L;
  }

  /*
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
  */

  /*
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

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder datasetName(String datasetName) {
      this.datasetName = datasetName;
      return this;
    }

    public Builder createdById(String createdById) {
      this.createdById = createdById;
      return this;
    }

    public Builder creationDate(ZonedDateTime creationDate) {
      this.creationDate = creationDate;
      return this;
    }

    public Builder language(Language language) {
      this.language = language;
      return this;
    }

    public Builder country(Country country) {
      this.country = country;
      return this;
    }


    public Builder harvestProtocol(HarvestProtocol harvestProtocol) {
      this.harvestProtocol = harvestProtocol;
      return this;
    }

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

    public UserDatasetDto build() {
      return new UserDatasetDto(this);
    }
  }
  */

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public void setCreationDate(ZonedDateTime creationDate) {
    this.creationDate = creationDate;
  }

  public void setHarvestProtocol(HarvestProtocol harvestProtocol) {
    this.harvestProtocol = harvestProtocol;
  }

  public void setProcessedRecords(Long processedRecords) {
    this.processedRecords = processedRecords;
  }

  public void setTotalRecords(Long totalRecords) {
    this.totalRecords = totalRecords;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  /**
   * Gets the dataset id.
   *
   * @return the dataset id
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
