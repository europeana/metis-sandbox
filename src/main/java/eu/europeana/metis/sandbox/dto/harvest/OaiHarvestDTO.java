package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

/**
 * Represents the data related to OAI-PMH harvesting
 */
@ApiModel(OaiHarvestDTO.SWAGGER_MODEL_NAME)
@Getter
public class OaiHarvestDTO extends HarvestParametersDTO {

    public static final String SWAGGER_MODEL_NAME = "OAIPmhUpload";

    @JsonProperty("url")
    private final String url;

    @JsonProperty("set-spec")
    private final String setSpec;

    @JsonProperty("metadata-format")
    private final String metadataFormat;

    /**
     * Constructor.
     *
     * @param url the URL of the OAI-PMH endpoint.
     * @param setSpec the set specification indicating the subset of records to harvest.
     * @param metadataFormat the metadata format used for harvesting.
     * @param stepSize the step size for harvesting.
     */
    public OaiHarvestDTO(String url, String setSpec, String metadataFormat, Integer stepSize) {
        super(stepSize);
        this.url = url;
        this.setSpec = setSpec;
        this.metadataFormat = metadataFormat;
    }
}
