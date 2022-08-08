package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;

/**
 * Class to encapsulate the all statistics information related to content and metadata tier
 *
 */
public class TiersZeroInfo {

    @JsonProperty("content-tier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TierStatistics contentTier;
    @JsonProperty("metadata-tier")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TierStatistics metadataTier;

    /**
     * Constructor
     * @param contentTier The content tier information
     * @param metadataTier The metadata information
     */
    public TiersZeroInfo(TierStatistics contentTier, TierStatistics metadataTier) {
        this.contentTier = contentTier;
        this.metadataTier = metadataTier;
    }

    public TierStatistics getContentTier() {
        return contentTier;
    }

    public TierStatistics getMetadataTier() {
        return metadataTier;
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        if(!(o instanceof TiersZeroInfo)){
            return false;
        }

        TiersZeroInfo other = (TiersZeroInfo) o;

        return this.contentTier.equals(other.contentTier) &&
                this.metadataTier.equals(other.metadataTier);
    }


}
