package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to OAI-PMH harvesting
 */
@ApiModel(OAIPmhHarvestingDto.SWAGGER_MODEL_NAME)
public class OAIPmhHarvestingDto implements HarvestingParametricDto {

    public static final String SWAGGER_MODEL_NAME = "OAIPmhUpload";

    @JsonProperty("harvest-protocol")
    private final String harvestProtocol = HarvestProtocol.OAI_PMH.value();

    @JsonProperty("url")
    private final String url;

    @JsonProperty("set-spec")
    private final String setSpec;

    @JsonProperty("metadata-format")
    private final String metadataFormat;

    /**
     * Constructor of this Dto
     * @param url The url used for harvesting
     * @param setSpec The setspec used for this harvesting
     * @param metadataFormat The metadata format used for this harvesting
     */
    public OAIPmhHarvestingDto(String url, String setSpec, String metadataFormat) {
        this.url = url;
        this.setSpec = setSpec;
        this.metadataFormat = metadataFormat;
    }

    public HarvestProtocol getHarvestProtocol() {
        return HarvestProtocol.OAI_PMH;
    }

    public String getUrl() {
        return url;
    }

    public String getSetSpec() {
        return setSpec;
    }

    public String getMetadataFormat() {
        return metadataFormat;
    }
}
