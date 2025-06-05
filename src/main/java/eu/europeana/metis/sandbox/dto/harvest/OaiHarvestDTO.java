package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the data related to OAI-PMH harvesting
 */
@ApiModel(OaiHarvestDTO.SWAGGER_MODEL_NAME)
@Getter
@AllArgsConstructor
public class OaiHarvestDTO implements HarvestParametersDTO {

    public static final String SWAGGER_MODEL_NAME = "OAIPmhUpload";

    @JsonProperty("url")
    private final String url;

    @JsonProperty("set-spec")
    private final String setSpec;

    @JsonProperty("metadata-format")
    private final String metadataFormat;
}
