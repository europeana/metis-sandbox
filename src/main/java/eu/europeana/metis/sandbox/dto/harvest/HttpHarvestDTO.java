package eu.europeana.metis.sandbox.dto.harvest;

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

  private final String fileName;
  private final String fileType;

  @JsonIgnore
  private final byte[] fileContent;

  public HttpHarvestDTO(String url, String fileName, String fileType, byte[] fileContent) {
    this.url = url;
    this.fileName = fileName;
    this.fileType = fileType;
    this.fileContent = fileContent;
  }

  public HarvestProtocol getHarvestProtocol() {
    return HarvestProtocol.HTTP;
  }

  public String getUrl() {
    return url;
  }

  public byte[] getFileContent() {
    return fileContent;
  }

  public String getFileType() {
    return fileType;
  }

  public String getFileName() {
    return fileName;
  }
}
