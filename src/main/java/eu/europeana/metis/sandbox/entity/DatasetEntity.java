package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

/**
 * Entity to map dataset table
 */
@Entity
@Table(name = "dataset")
public class DatasetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer datasetId;
  private String datasetName;
  private String createdById;
  private Long recordsQuantity;

  @Column(insertable = false, updatable = false)
  private ZonedDateTime createdDate;

  @Enumerated(EnumType.STRING)
  private Language language;

  @Enumerated(EnumType.STRING)
  private Country country;

  @Column(columnDefinition="TEXT")
  private String xsltToEdmExternal;

  private Boolean recordLimitExceeded;

  private WorkflowType workflowType;

  /**
   * Constructs a DatasetEntity.
   *
   * @param datasetName Name of the dataset.
   * @param createdById Identifier of the dataset creator.
   * @param recordsQuantity The number of records in the dataset.
   * @param language The language associated with the dataset.
   * @param country The country associated with the dataset.
   * @param recordLimitExceeded A flag indicating whether the dataset exceeds the record limit.
   */
  public DatasetEntity(WorkflowType workflowType, String datasetName, String createdById, Long recordsQuantity, Language language, Country country,
      Boolean recordLimitExceeded) {
    this.workflowType = workflowType;
    this.datasetName = datasetName;
    this.createdById = createdById;
    this.recordsQuantity = recordsQuantity;
    this.language = language;
    this.country = country;
    this.recordLimitExceeded = recordLimitExceeded;

  }

  /**
   * Constructs a DatasetEntity.
   *
   * @param datasetName Name of the dataset.
   * @param createdById Identifier of the dataset creator.
   * @param recordsQuantity The number of records in the dataset.
   * @param language The language associated with the dataset.
   * @param country The country associated with the dataset.
   * @param recordLimitExceeded A flag indicating whether the dataset exceeds the record limit.
   * @param xsltToEdmExternal External XSLT EDM content associated with the dataset.
   */
  public DatasetEntity(WorkflowType workflowType, String datasetName, String createdById, Long recordsQuantity, Language language, Country country,
      Boolean recordLimitExceeded, String xsltToEdmExternal) {
    this(workflowType, datasetName, createdById, recordsQuantity, language, country, recordLimitExceeded);
    this.xsltToEdmExternal = xsltToEdmExternal;
  }

  public DatasetEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Integer getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Integer datasetId) {
    this.datasetId = datasetId;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public String getCreatedById() {
    return createdById;
  }

  public void setCreatedById(String createdById) {
    this.createdById = createdById;
  }

  public Long getRecordsQuantity() {
    return recordsQuantity;
  }

  public void setRecordsQuantity(Long recordsQuantity) {
    this.recordsQuantity = recordsQuantity;
  }

  public ZonedDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(ZonedDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public Boolean getRecordLimitExceeded() {
    return recordLimitExceeded;
  }

  public void setRecordLimitExceeded(Boolean hasReachedRecordLimit) {
    this.recordLimitExceeded = hasReachedRecordLimit;
  }

  public String getXsltToEdmExternal() {
    return xsltToEdmExternal;
  }

  public void setXsltToEdmExternal(String xsltTransformerEdmExternal) {
    this.xsltToEdmExternal = xsltTransformerEdmExternal;
  }

  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }
}
