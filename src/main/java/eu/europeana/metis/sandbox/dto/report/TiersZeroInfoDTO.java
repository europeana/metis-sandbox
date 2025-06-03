package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Class to encapsulate the all statistics information related to content and metadata tier
 *
 */
public class TiersZeroInfoDTO {

    @JsonProperty("content-tier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final TierStatisticsDTO contentTier;
    @JsonProperty("metadata-tier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final TierStatisticsDTO metadataTier;

    /**
     * Constructor
     * @param contentTier The content tier information
     * @param metadataTier The metadata information
     */
    public TiersZeroInfoDTO(TierStatisticsDTO contentTier, TierStatisticsDTO metadataTier) {
        this.contentTier = contentTier;
        this.metadataTier = metadataTier;
    }

    public TierStatisticsDTO getContentTier() {
        return contentTier;
    }

    public TierStatisticsDTO getMetadataTier() {
        return metadataTier;
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        if(!(o instanceof TiersZeroInfoDTO other)){
            return false;
        }

      return this.contentTier.equals(other.contentTier) && this.metadataTier.equals(other.metadataTier);
    }

    @Override
    public int hashCode(){
        return Objects.hash(contentTier, metadataTier);
    }

}
