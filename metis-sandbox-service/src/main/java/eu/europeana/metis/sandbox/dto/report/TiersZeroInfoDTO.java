package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class to encapsulate the all statistics information related to content and metadata tier
 */
public record TiersZeroInfoDTO(

    @JsonProperty("content-tier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    TierStatisticsDTO contentTier,

    @JsonProperty("metadata-tier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    TierStatisticsDTO metadataTier
) {

}
