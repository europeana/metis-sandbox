package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import io.swagger.annotations.ApiModel;

/**
 * Represents the data related to File harvesting
 */
@ApiModel(FileHarvestDTO.SWAGGER_MODEL_NAME)
public class FileHarvestDTO implements HarvestParametersDTO {

  public static final String SWAGGER_MODEL_NAME = "FileUpload";

  @JsonProperty("harvest-protocol")
  private final String harvestProtocol = HarvestProtocol.FILE.value();

  @JsonProperty("file-name")
  private final String fileName;

  @JsonProperty("file-type")
  private final String fileType;

  @JsonIgnore
  private final byte[] fileContent;

  /**
   * Constructor of this Dto
   *
   * @param fileName The name of the file used for harvesting
   * @param fileType The name of file uploaded (e.g., zip, tar,...)
   * @param xsltToExternal
   */
  public FileHarvestDTO(String fileName, String fileType, byte[] fileContent) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.fileContent = fileContent;
  }

  public HarvestProtocol getHarvestProtocol() {
    return HarvestProtocol.FILE;
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public byte[] getFileContent() {
    return fileContent;
  }
}
