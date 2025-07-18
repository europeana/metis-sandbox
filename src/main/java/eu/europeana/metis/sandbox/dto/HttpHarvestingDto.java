package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.annotations.ApiModel;
import java.util.Objects;

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

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof HttpHarvestingDto that)) {
            return false;
        }

      return Objects.equals(harvestProtocol, that.harvestProtocol) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
       return Objects.hash(harvestProtocol);
    }
}
