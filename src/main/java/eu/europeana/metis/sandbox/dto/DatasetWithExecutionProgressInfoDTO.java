package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import io.swagger.annotations.ApiModel;

/**
 * Represents a dataset along with its execution progress information.
 * <p>
 * This class serves as a wrapper to combine dataset metadata and its corresponding execution progress in one unified structure
 * for API communication or internal data processing.
 */
@ApiModel(DatasetWithExecutionProgressInfoDTO.SWAGGER_MODEL_NAME)
public record DatasetWithExecutionProgressInfoDTO(@JsonProperty("dataset-info") DatasetInfoDTO datasetInfo,
                                                  @JsonProperty("execution-progress-info") ExecutionProgressInfoDTO executionProgressInfo) {

  public static final String SWAGGER_MODEL_NAME = "DatasetWithExecution";
}
