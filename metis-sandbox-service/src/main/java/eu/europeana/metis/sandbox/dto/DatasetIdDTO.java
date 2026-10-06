package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a dataset identifier.
 *
 * <p>Used to encapsulate dataset identification details for API communication.
 */
@Schema(name = DatasetIdDTO.SWAGGER_MODEL_NAME)
public record DatasetIdDTO(@JsonProperty(value = "dataset-id") String datasetId) {

  public static final String SWAGGER_MODEL_NAME = "Dataset";

}
