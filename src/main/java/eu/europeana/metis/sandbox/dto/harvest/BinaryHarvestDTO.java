package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class BinaryHarvestDTO implements HarvestParametersDTO {

  @JsonProperty("file-name")
  private final String fileName;

  @JsonProperty("file-type")
  private final FileType fileType;

  @JsonIgnore
  private final byte[] fileContent;
}
