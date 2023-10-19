package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(OAIPmhHarvestingDto.SWAGGER_MODEL_NAME)
public class OAIPmhHarvestingDto implements HarvestingParametersDto {

    public static final String SWAGGER_MODEL_NAME = "OAIPmhUpload";

    @JsonProperty("protocol")
    private final String protocol = "OAI-PMH";

    @JsonProperty("url")
    private final String url;

    @JsonProperty("set-spec")
    private final String setSpec;

    @JsonProperty("metadata-format")
    private final String metadataFormat;

    public OAIPmhHarvestingDto(String url, String setSpec, String metadataFormat) {
        this.url = url;
        this.setSpec = setSpec;
        this.metadataFormat = metadataFormat;
    }

    public String getProtocol() {
        return protocol;
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
