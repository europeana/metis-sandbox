package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;

@ApiModel(HttpUploadDto.SWAGGER_MODEL_NAME)
public class HttpUploadDto implements UploadDataDto{

    public static final String SWAGGER_MODEL_NAME = "HttpUpload";

    @JsonProperty("protocol")
    private final String protocol = "HTTP";

    @JsonProperty("url")
    private final String url;

    public HttpUploadDto(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getUrl() {
        return url;
    }
}
