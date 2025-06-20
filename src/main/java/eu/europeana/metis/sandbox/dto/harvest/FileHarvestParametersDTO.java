package eu.europeana.metis.sandbox.dto.harvest;

import eu.europeana.metis.sandbox.common.FileType;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

/**
 * Represents the data related to File harvesting
 */
@ApiModel(FileHarvestParametersDTO.SWAGGER_MODEL_NAME)
@Getter
public class FileHarvestParametersDTO extends AbstractBinaryHarvestParametersDTO {

  public static final String SWAGGER_MODEL_NAME = "FileUpload";

  /**
   * Constructor.
   *
   * @param fileName the name of the file.
   * @param fileType the type of the file.
   * @param fileContent the binary content of the file to be harvested.
   * @param stepSize the step size for harvesting.
   */
  public FileHarvestParametersDTO(String fileName, FileType fileType, byte[] fileContent, Integer stepSize) {
    super(fileName, fileType, fileContent, stepSize);
  }
}
