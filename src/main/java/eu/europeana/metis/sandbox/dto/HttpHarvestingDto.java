package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Protocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to Http harvesting
 */
@ApiModel(HttpHarvestingDto.SWAGGER_MODEL_NAME)
public class HttpHarvestingDto implements HarvestingParametersDto {

    public static final String SWAGGER_MODEL_NAME = "HttpUpload";

    @JsonProperty("protocol")
    private final Protocol protocol = Protocol.HTTP;

    @JsonProperty("url")
    private final String url;

    /**
     * Constructor of this Dto
     * @param url The url used for harvesting
     */
    public HttpHarvestingDto(String url) {
        this.url = url;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public String getUrl() {
        return url;
    }
}
