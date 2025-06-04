package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to Http harvesting
 */
@ApiModel(HttpHarvestDTO.SWAGGER_MODEL_NAME)
public class HttpHarvestDTO implements HarvestParametersDTO {

  public static final String SWAGGER_MODEL_NAME = "HttpUpload";

  @JsonProperty("harvest-protocol")
  private final String harvestProtocol = HarvestProtocol.HTTP.value();

  @JsonProperty("url")
  private final String url;

  private final String fileType;

  @JsonIgnore
  private final byte[] fileData;

  public HttpHarvestDTO(String url, String fileType, byte[] fileData) {
    this.url = url;
    this.fileType = fileType;
    this.fileData = fileData;
  }

  public HarvestProtocol getHarvestProtocol() {
    return HarvestProtocol.HTTP;
  }

  public String getUrl() {
    return url;
  }

  public byte[] getFileData() {
    return fileData;
  }

  public String getFileType() {
    return fileType;
  }
}
