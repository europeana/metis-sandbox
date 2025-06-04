package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.europeana.metis.sandbox.common.HarvestProtocol;

/**
 * Interface that represents all type of harvesting with their parameters
 */
public interface HarvestParametersDTO {

    @JsonIgnore
    HarvestProtocol getHarvestProtocol();
}
