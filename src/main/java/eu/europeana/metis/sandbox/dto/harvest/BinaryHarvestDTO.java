package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.FileType;
import lombok.Getter;

@Getter
public abstract class BinaryHarvestDTO extends HarvestParametersDTO {

  @JsonProperty("file-name")
  private final String fileName;

  @JsonProperty("file-type")
  private final FileType fileType;

  @JsonIgnore
  private final byte[] fileContent;

  public BinaryHarvestDTO(String fileName, FileType fileType, byte[] fileContent, Integer stepSize) {
    super(stepSize);
    this.fileName = fileName;
    this.fileType = fileType;
    this.fileContent = fileContent;
  }
}
