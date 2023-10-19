package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(HttpHarvestingDto.SWAGGER_MODEL_NAME)
public class HttpHarvestingDto implements HarvestingParametersDto {

    public static final String SWAGGER_MODEL_NAME = "HttpUpload";

    @JsonProperty("protocol")
    private final String protocol = "HTTP";

    @JsonProperty("url")
    private final String url;

    public HttpHarvestingDto(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUrl() {
        return url;
    }
}
