package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.batch.entity.ExecutionRecordTierContext;
import io.swagger.annotations.ApiModel;

@ApiModel(RecordTiersInfoDTO.SWAGGER_MODEL_NAME)
/**
 * Object to encapsulate all tiers values related to a record
 */
public class RecordTiersInfoDTO {

  public static final String SWAGGER_MODEL_NAME = "RecordTiersInfo";

  @JsonProperty("record-id")
  private final String recordId;

  @JsonProperty("content-tier")
  private final MediaTier contentTier;

  @JsonProperty("content-tier-before-license-correction")
  private final MediaTier contentTierBeforeLicenseCorrection;

  @JsonProperty("license")
  private final LicenseType license;

  @JsonProperty("metadata-tier")
  private final MetadataTier metadataTier;

  @JsonProperty("metadata-tier-language")
  private final MetadataTier metadataTierLanguage;

  @JsonProperty("metadata-tier-enabling-elements")
  private final MetadataTier metadataTierEnablingElements;

  @JsonProperty("metadata-tier-contextual-classes")
  private final MetadataTier metadataTierContextualClasses;

  public RecordTiersInfoDTO(ExecutionRecordTierContext executionRecordTierContext) {
    recordId = executionRecordTierContext.getIdentifier().getRecordId();
    contentTier = MediaTier.getEnum(executionRecordTierContext.getContentTier());
    contentTierBeforeLicenseCorrection = MediaTier.getEnum(executionRecordTierContext.getContentTierBeforeLicenseCorrection());
    license = LicenseType.valueOf(executionRecordTierContext.getLicense());
    metadataTier = MetadataTier.getEnum(executionRecordTierContext.getMetadataTier());
    metadataTierLanguage = MetadataTier.getEnum(executionRecordTierContext.getMetadataTierLanguage());
    metadataTierEnablingElements = MetadataTier.getEnum(executionRecordTierContext.getMetadataTierEnablingElements());
    metadataTierContextualClasses = MetadataTier.getEnum(executionRecordTierContext.getMetadataTierContextualClasses());
  }

  /**
   * Constructor using the builder
   *
   * @param builder The builder
   */
  public RecordTiersInfoDTO(RecordTiersInfoDtoBuilder builder) {
    recordId = builder.recordId;
    contentTier = builder.contentTier;
    contentTierBeforeLicenseCorrection = builder.contentTierBeforeLicenseCorrection;
    license = builder.license;
    metadataTier = builder.metadataTier;
    metadataTierLanguage = builder.metadataTierLanguage;
    metadataTierEnablingElements = builder.metadataTierEnablingElements;
    metadataTierContextualClasses = builder.metadataTierContextualClasses;
  }

  public String getRecordId() {
    return recordId;
  }

  public MediaTier getContentTier() {
    return contentTier;
  }

  public MediaTier getContentTierBeforeLicenseCorrection() {
    return contentTierBeforeLicenseCorrection;
  }

  public LicenseType getLicense() {
    return license;
  }

  public MetadataTier getMetadataTier() {
    return metadataTier;
  }

  public MetadataTier getMetadataTierLanguage() {
    return metadataTierLanguage;
  }

  public MetadataTier getMetadataTierEnablingElements() {
    return metadataTierEnablingElements;
  }

  public MetadataTier getMetadataTierContextualClasses() {
    return metadataTierContextualClasses;
  }

  public static class RecordTiersInfoDtoBuilder {

    private String recordId;
    private MediaTier contentTier;
    private MediaTier contentTierBeforeLicenseCorrection;
    private LicenseType license;
    private MetadataTier metadataTier;
    private MetadataTier metadataTierLanguage;
    private MetadataTier metadataTierEnablingElements;
    private MetadataTier metadataTierContextualClasses;

    public RecordTiersInfoDtoBuilder() {
    }

    /**
     * Sets the value of record id
     *
     * @param recordId The id of the record to represent
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setRecordId(String recordId) {
      this.recordId = recordId;
      return this;
    }

    /**
     * Sets the value of content tier
     *
     * @param contentTier The value of the content tier of the record
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setContentTier(MediaTier contentTier) {
      this.contentTier = contentTier;
      return this;
    }

    /**
     * Sets the value of content tier before license correction
     *
     * @param contentTierBeforeLicenseCorrection The value of the content tier before license correction
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setContentTierBeforeLicenseCorrection(MediaTier contentTierBeforeLicenseCorrection) {
      this.contentTierBeforeLicenseCorrection = contentTierBeforeLicenseCorrection;
      return this;
    }

    /**
     * Sets the value of license type
     *
     * @param license The license type of the record
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setLicense(LicenseType license) {
      this.license = license;
      return this;
    }

    /**
     * Sets the value of metadata tier
     *
     * @param metadataTier The value of the metadata tier of the record
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setMetadataTier(MetadataTier metadataTier) {
      this.metadataTier = metadataTier;
      return this;
    }

    /**
     * Sets the value of metadata tier related to Language class
     *
     * @param metadataTierLanguage The value of the metadata tier related to Language class
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setMetadataTierLanguage(MetadataTier metadataTierLanguage) {
      this.metadataTierLanguage = metadataTierLanguage;
      return this;
    }

    /**
     * Sets the value of metadata tier related to Enabling Elements class
     *
     * @param metadataTierEnablingElements The value of the metadata tier related to Enabling Elements class
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setMetadataTierEnablingElements(MetadataTier metadataTierEnablingElements) {
      this.metadataTierEnablingElements = metadataTierEnablingElements;
      return this;
    }

    /**
     * Sets the value of metadata tier related to Contextual Classes class
     *
     * @param metadataTierContextualClasses The value of the metadata tier related to Contextual Classes class
     * @return The builder with the new value
     */
    public RecordTiersInfoDtoBuilder setMetadataTierContextualClasses(MetadataTier metadataTierContextualClasses) {
      this.metadataTierContextualClasses = metadataTierContextualClasses;
      return this;
    }

    /**
     * Build the object RecordTiersInfoDto based on the given values from the builder
     *
     * @return a new RecordTiersInfoDto object
     */
    public RecordTiersInfoDTO build() {
      return new RecordTiersInfoDTO(this);
    }

  }
}
