package eu.europeana.metis.sandbox.entity;

import eu.europeana.metis.sandbox.domain.Record;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;

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
   * Constructor using the builder
   * @param builder the builder
   */
  public RecordEntity(RecordEntityBuilder builder){
    this.europeanaId = builder.europeanaId;
    this.providerId = builder.providerId;
    this.datasetId = builder.datasetId;
    this.contentTier = builder.contentTier;
    this.contentTierBeforeLicenseCorrection = builder.contentTierBeforeLicenseCorrection;
    this.metadataTier = builder.metadataTier;
    this.metadataTierLanguage = builder.metadataTierLanguage;
    this.metadataTierEnablingElements = builder.metadataTierEnablingElements;
    this.metadataTierContextualClasses = builder.metadataTierContextualClasses;
    this.license = builder.license;
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
    this.contentTier = null;
    this.contentTierBeforeLicenseCorrection = null;
    this.metadataTier = null;
    this.metadataTierLanguage = null;
    this.metadataTierEnablingElements = null;
    this.metadataTierContextualClasses = null;
    this.license = null;
  }

  /**
   * Constructor with id parameters. The remaining of the fields will be null
   * @param providerId the provider id associated to the record
   * @param datasetId the dataset if it belongs to
   */
  public RecordEntity(String providerId, String datasetId){
    this.europeanaId = null;
    this.providerId = providerId;
    this.datasetId = datasetId;
    this.contentTier = null;
    this.contentTierBeforeLicenseCorrection = null;
    this.metadataTier = null;
    this.metadataTierLanguage = null;
    this.metadataTierEnablingElements = null;
    this.metadataTierContextualClasses = null;
    this.license = null;
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

  public static class RecordEntityBuilder{

    private String europeanaId;
    private String providerId;
    private String datasetId;
    private String contentTier;
    private String contentTierBeforeLicenseCorrection;
    private String metadataTier;
    private String metadataTierLanguage;
    private String metadataTierEnablingElements;
    private String metadataTierContextualClasses;
    private String license;

    /**
     * Constructor
     */
    public RecordEntityBuilder(){
    }

    /**
     * Sets the value of europeana id
     * @param europeanaId the europeana id associated to the record
     * @return the builder with the new value
     */
    public RecordEntityBuilder setEuropeanaId(String europeanaId){
      this.europeanaId = europeanaId;
      return this;
    }

    /**
     * Sets the value of provider id
     * @param providerId the id of the record provided
     * @return the builder with the new value
     */
    public RecordEntityBuilder setProviderId(String providerId){
      this.providerId = providerId;
      return this;
    }

    /**
     * Sets the value of dataset id
     * @param datasetId the dataset id associated to the record
     * @return the builder with the new value
     */
    public RecordEntityBuilder setDatasetId(String datasetId){
      this.datasetId = datasetId;
      return this;
    }

    /**
     * Sets the value of content tier
     * @param contentTier the value of the content tier of the record
     * @return the builder with the new value
     */
    public RecordEntityBuilder setContentTier(String contentTier){
      this.contentTier = contentTier;
      return this;
    }

    /**
     * Sets the value of content tier before license correction
     * @param contentTierBeforeLicenseCorrection  the value of the content tier before license correction
     * @return the builder with the new value
     */
    public RecordEntityBuilder setContentTierBeforeLicenseCorrection(String contentTierBeforeLicenseCorrection){
      this.contentTierBeforeLicenseCorrection = contentTierBeforeLicenseCorrection;
      return this;
    }

    /**
     * Sets the value of metadata tier
     * @param metadataTier the value of the metadata tier of the record
     * @return the builder with the new value
     */
    public RecordEntityBuilder setMetadataTier(String metadataTier){
      this.metadataTier = metadataTier;
      return this;
    }

    /**
     * Sets the value of metadata tier related to Language class
     * @param metadataTierLanguage the value of the metadata tier related to Language class
     * @return the builder with the new value
     */
    public RecordEntityBuilder setMetadataTierLanguage(String metadataTierLanguage){
      this.metadataTierLanguage = metadataTierLanguage;
      return this;
    }

    /**
     * Sets the value of metadata tier related to Enabling Elements class
     * @param metadataTierEnablingElements the value of the metadata tier related to Enabling Elements class
     * @return the builder with the new value
     */
    public RecordEntityBuilder setMetadataTierEnablingElements(String metadataTierEnablingElements){
      this.metadataTierEnablingElements = metadataTierEnablingElements;
      return this;
    }

    /**
     * Sets the value of metadata tier related to Contextual Classes class
     * @param metadataTierContextualClasses the value of the metadata tier related to Contextual Classes class
     * @return the builder with the new value
     */
    public RecordEntityBuilder setMetadataTierContextualClasses(String metadataTierContextualClasses){
      this.metadataTierContextualClasses = metadataTierContextualClasses;
      return this;
    }

    /**
     * Sets the value of license type
     * @param license the value of the license type
     * @return the builder with the new value
     */
    public RecordEntityBuilder setLicense(String license){
      this.license = license;
      return this;
    }

    /**
     * Builds a new RecordEntity based on the values given to the builder
     * @return a new RecordEntity object
     */
    public RecordEntity build(){
      return new RecordEntity(this);
    }

  }
}
