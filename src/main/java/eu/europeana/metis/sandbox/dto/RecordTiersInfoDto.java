package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.utils.LicenseType;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import io.swagger.annotations.ApiModel;

@ApiModel(RecordTiersInfoDto.SWAGGER_MODEL_NAME)

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
