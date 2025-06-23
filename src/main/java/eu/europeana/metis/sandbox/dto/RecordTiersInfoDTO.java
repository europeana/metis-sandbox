package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.indexing.tiers.model.MediaTier;
import eu.europeana.indexing.tiers.model.MetadataTier;
import eu.europeana.indexing.utils.LicenseType;
import io.swagger.annotations.ApiModel;
import lombok.Builder;

/**
 * Object to encapsulate all tiers values related to a record
 */
@ApiModel(RecordTiersInfoDTO.SWAGGER_MODEL_NAME)
@Builder
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
}

