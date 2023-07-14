package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import io.swagger.annotations.ApiModel;

@ApiModel(RecordTiersInfoDto.SWAGGER_MODEL_NAME)
/**
 * Object to encapsulate all tiers values related to a record
 */
public class RecordTiersInfoDto {

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

    /**
     * Constructor of the object to initialize each field
     * @param recordId The id of the record to represent
     * @param contentTier The value of the content tier of the record
     * @param contentTierBeforeLicenseCorrection The value of the content tier before license correction
     * @param license The license type of the record
     * @param metadataTier The value of the metadata tier of the record
     * @param metadataTierLanguage The value of the metadata tier related to Language class
     * @param metadataTierEnablingElements The value of the metadata tier related to Enabling Elements class
     * @param metadataTierContextualClasses The value of the metadata tier related to Contextual Classes class
     */
    public RecordTiersInfoDto(String recordId, MediaTier contentTier, MediaTier contentTierBeforeLicenseCorrection,
                              LicenseType license, MetadataTier metadataTier, MetadataTier metadataTierLanguage,
                              MetadataTier metadataTierEnablingElements, MetadataTier metadataTierContextualClasses) {
        this.recordId = recordId;
        this.contentTier = contentTier;
        this.contentTierBeforeLicenseCorrection = contentTierBeforeLicenseCorrection;
        this.license = license;
        this.metadataTier = metadataTier;
        this.metadataTierLanguage = metadataTierLanguage;
        this.metadataTierEnablingElements = metadataTierEnablingElements;
        this.metadataTierContextualClasses = metadataTierContextualClasses;
    }

    /**
     * Constructor of the object using a RecordEntity
     * @param recordEntity The record entity to gather the information from
     */
    public RecordTiersInfoDto(RecordEntity recordEntity){
        recordId = recordEntity.getEuropeanaId();
        contentTier = MediaTier.getEnum(recordEntity.getContentTier());
        contentTierBeforeLicenseCorrection = MediaTier.getEnum(recordEntity.getContentTierBeforeLicenseCorrection());
        license = LicenseType.valueOf(recordEntity.getLicense());
        metadataTier = MetadataTier.getEnum(recordEntity.getMetadataTier());
        metadataTierLanguage = MetadataTier.getEnum(recordEntity.getMetadataTierLanguage());
        metadataTierEnablingElements = MetadataTier.getEnum(recordEntity.getMetadataTierEnablingElements());
        metadataTierContextualClasses = MetadataTier.getEnum(recordEntity.getMetadataTierContextualClasses());
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
}
