package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to Http harvesting
 */
@ApiModel(HttpHarvestingDto.SWAGGER_MODEL_NAME)
public class HttpHarvestingDto implements HarvestingParametricDto {

    public static final String SWAGGER_MODEL_NAME = "HttpUpload";

    @JsonProperty("harvest-protocol")
    private final String harvestProtocol = HarvestProtocol.HTTP.value();

    @JsonProperty("url")
    private final String url;

    /**
     * Constructor of this Dto
     * @param url The url used for harvesting
     */
    public HttpHarvestingDto(String url) {
        this.url = url;
    }

    public HarvestProtocol getHarvestProtocol() {
        return HarvestProtocol.HTTP;
    }

    public String getUrl() {
        return url;
    }
}
