package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Interface that represents all type of harvesting with their parameters
 */
@Getter
@AllArgsConstructor
public abstract class HarvestParametersDTO {

  @JsonProperty("step-size")
  private Integer stepSize;
}
