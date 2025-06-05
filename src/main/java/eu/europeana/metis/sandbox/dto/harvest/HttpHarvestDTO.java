package eu.europeana.metis.sandbox.dto.harvest;

import eu.europeana.metis.sandbox.common.FileType;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

/**
 * Represents the data related to Http harvesting
 */
@ApiModel(HttpHarvestDTO.SWAGGER_MODEL_NAME)
@Getter
public class HttpHarvestDTO extends BinaryHarvestDTO {

  public static final String SWAGGER_MODEL_NAME = "HttpUpload";

  private final String url;

  public HttpHarvestDTO(String url, String fileName, FileType fileType, byte[] fileContent, Integer stepSize) {
    super(fileName, fileType, fileContent, stepSize);
    this.url = url;
  }
}
