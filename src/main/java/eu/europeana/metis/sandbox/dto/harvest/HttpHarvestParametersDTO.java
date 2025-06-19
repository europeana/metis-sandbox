package eu.europeana.metis.sandbox.dto.harvest;

import eu.europeana.metis.sandbox.common.FileType;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

/**
 * Represents the data related to Http harvesting
 */
@ApiModel(HttpHarvestParametersDTO.SWAGGER_MODEL_NAME)
@Getter
public class HttpHarvestParametersDTO extends BinaryHarvestParametersDTO {

  public static final String SWAGGER_MODEL_NAME = "HttpUpload";

  private final String url;

  /**
   * Constructor.
   *
   * @param url the URL associated with the file to harvest.
   * @param fileName the name of the file being harvested.
   * @param fileType the type of the file being harvested.
   * @param fileContent the binary content of the file to be harvested.
   * @param stepSize the step size for harvesting.
   */
  public HttpHarvestParametersDTO(String url, String fileName, FileType fileType, byte[] fileContent, Integer stepSize) {
    super(fileName, fileType, fileContent, stepSize);
    this.url = url;
  }
}
