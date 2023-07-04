package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.domain.Record;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Entity to map to record table
 */
@Entity
@Table(name = "record")
public class RecordEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  protected Long id;

  protected String europeanaId;

  protected String providerId;

  protected String datasetId;

  protected String contentTier;

  protected String contentTierBeforeLicenseCorrection;

  protected String metadataTier;

  protected String metadataTierLanguage;

  protected String metadataTierEnablingElements;

  protected String metadataTierContextualClasses;

  protected String license;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "recordId")
  private List<RecordLogEntity> recordLogEntity;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "recordId")
  private List<RecordErrorLogEntity> recordErrorLogEntity;

  /**
   * Parameterized constructor
   *
   * @param europeanaId the europeana id associated to the record
   * @param providerId the id of the record provided
   * @param datasetId the dataset id associated to the record
   * @param contentTier the value of the content tier of the record
   * @param metadataTier the value of the metadata tier of the record
   */
  public RecordEntity(String europeanaId, String providerId, String datasetId, String contentTier, String contentTierBeforeLicenseCorrection,
                      String metadataTier, String metadataTierLanguage, String metadataTierEnablingElements, String metadataTierContextualClasses,
                      String license) {
    this.europeanaId = europeanaId;
    this.providerId = providerId;
    this.datasetId = datasetId;
    this.contentTier = contentTier;
    this.contentTierBeforeLicenseCorrection = contentTierBeforeLicenseCorrection;
    this.metadataTier = metadataTier;
    this.metadataTierLanguage = metadataTierLanguage;
    this.metadataTierEnablingElements = metadataTierEnablingElements;
    this.metadataTierContextualClasses = metadataTierContextualClasses;
    this.license = license;
  }

  /**
   * Constructor
   *
   * @param recordInput the record
   */
  public RecordEntity(Record recordInput){
    this.europeanaId = recordInput.getEuropeanaId();
    this.providerId = recordInput.getProviderId();
    this.datasetId = recordInput.getDatasetId();
    this.contentTier = "";
    this.metadataTier = "";
  }


  public RecordEntity() {
    // provide explicit no-args constructor as it is required for Hibernate
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEuropeanaId() {
    return europeanaId;
  }

  public void setEuropeanaId(String europeanaId) {
    this.europeanaId = europeanaId;
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public String getContentTier() {
    return contentTier;
  }

  public void setContentTier(String contentTier) {
    this.contentTier = contentTier;
  }

  public String getContentTierBeforeLicenseCorrection() {
    return contentTierBeforeLicenseCorrection;
  }

  public void setContentTierBeforeLicenseCorrection(String contentTierBeforeLicenseCorrection) {
    this.contentTierBeforeLicenseCorrection = contentTierBeforeLicenseCorrection;
  }

  public String getMetadataTier() {
    return metadataTier;
  }

  public void setMetadataTier(String metadataTier) {
    this.metadataTier = metadataTier;
  }

  public String getMetadataTierLanguage() {
    return metadataTierLanguage;
  }

  public void setMetadataTierLanguage(String metadataTierLanguage) {
    this.metadataTierLanguage = metadataTierLanguage;
  }

  public String getMetadataTierEnablingElements() {
    return metadataTierEnablingElements;
  }

  public void setMetadataTierEnablingElements(String metadataTierEnablingElements) {
    this.metadataTierEnablingElements = metadataTierEnablingElements;
  }

  public String getMetadataTierContextualClasses() {
    return metadataTierContextualClasses;
  }

  public void setMetadataTierContextualClasses(String metadataTierContextualClasses) {
    this.metadataTierContextualClasses = metadataTierContextualClasses;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public List<RecordLogEntity> getRecordLogEntity() {
    return recordLogEntity;
  }

  public void setRecordLogEntity(
      List<RecordLogEntity> recordLogEntity) {
    this.recordLogEntity = recordLogEntity;
  }

  public List<RecordErrorLogEntity> getRecordErrorLogEntity() {
    return recordErrorLogEntity;
  }

  public void setRecordErrorLogEntity(
      List<RecordErrorLogEntity> recordErrorLogEntity) {
    this.recordErrorLogEntity = recordErrorLogEntity;
  }
}
