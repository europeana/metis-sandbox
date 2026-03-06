package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Represents the data related to OAI-PMH harvesting
 */
@Schema(name = OaiHarvestParametersDTO.SWAGGER_MODEL_NAME)
@Getter
public class OaiHarvestParametersDTO extends AbstractHarvestParametersDTO {

  public static final String SWAGGER_MODEL_NAME = "OAIPmhUpload";
  public static final HarvestProtocol HARVEST_PROTOCOL = HarvestProtocol.OAI;

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
  public OaiHarvestParametersDTO(String url, String setSpec, String metadataFormat, Integer stepSize) {
    super(HARVEST_PROTOCOL, stepSize);
    this.url = url;
    this.setSpec = setSpec;
    this.metadataFormat = metadataFormat;
  }
}
