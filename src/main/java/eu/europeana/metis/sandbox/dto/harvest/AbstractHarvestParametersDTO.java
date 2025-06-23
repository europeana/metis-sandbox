package eu.europeana.metis.sandbox.dto.harvest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Interface that represents all types of harvesting with their parameters.
 */
@Getter
@AllArgsConstructor
@SuppressWarnings("java:S1694") // This is intentional. It's a shared field(s) container class for subclasses.
public abstract class AbstractHarvestParametersDTO {

  @JsonProperty("step-size")
  private Integer stepSize;
}
