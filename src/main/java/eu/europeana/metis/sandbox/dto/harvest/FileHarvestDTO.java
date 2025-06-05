package eu.europeana.metis.sandbox.dto.harvest;

import eu.europeana.metis.sandbox.common.FileType;
import io.swagger.annotations.ApiModel;
import lombok.Getter;

/**
 * Represents the data related to File harvesting
 */
@ApiModel(FileHarvestDTO.SWAGGER_MODEL_NAME)
@Getter
public class FileHarvestDTO extends BinaryHarvestDTO {

  public static final String SWAGGER_MODEL_NAME = "FileUpload";

  public FileHarvestDTO(String fileName, FileType fileType, byte[] fileContent) {
    super(fileName, fileType, fileContent);
  }

}
