package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.FileType;
import java.util.Arrays;
import lombok.Getter;

/**
 * Represents the base class for harvesting binary data with file parameters.
 */
@Getter
public abstract class AbstractBinaryHarvestParametersDTO extends AbstractHarvestParametersDTO {

  @JsonProperty("file-name")
  private final String fileName;

  @JsonProperty("file-type")
  private final FileType fileType;

  @JsonIgnore
  private final byte[] fileContent;

  /**
   * Constructor.
   *
   * @param fileName the name of the file.
   * @param fileType the type of the file.
   * @param fileContent the binary content of the file to be harvested.
   * @param stepSize the step size for harvesting.
   */
  protected AbstractBinaryHarvestParametersDTO(String fileName, FileType fileType, byte[] fileContent, Integer stepSize) {
    super(stepSize);
    this.fileName = fileName;
    this.fileType = fileType;
    this.fileContent = Arrays.copyOf(fileContent, fileContent.length);
  }
}
