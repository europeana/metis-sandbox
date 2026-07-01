package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Represents the execution progress information for a dataset processing workflow.
 */
@Schema(name = ExecutionProgressInfoDTO.PROGRESS_SWAGGER_MODEL_NAME)
public record ExecutionProgressInfoDTO(
    //todo: (https://europeana.atlassian.net/browse/MET-7162) remove when the preview field is used in the ui(https://europeana.atlassian.net/browse/MET-7161)
    @JsonProperty("portal-publish")
    String portalUrlPublish,

    @JsonProperty("portal-preview")
    String portalUrlPreview,

    @JsonProperty("status")
    ExecutionStatus executionStatus,

    @JsonProperty("total-records")
    long totalRecords,

    @JsonProperty("processed-records")
    long processedRecords,

    @JsonProperty("progress-by-step")
    List<ExecutionProgressByStepDTO> executionProgressByStepDTOS,

    @JsonProperty("record-limit-exceeded")
    boolean recordLimitExceeded,

    @JsonProperty("dataset-logs")
    List<DatasetErrorInfoDTO> datasetErrorInfoDTOS,

    @JsonProperty("tier-zero-info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    TiersZeroInfoDTO tiersZeroInfoDTO
) {

  public static final String PROGRESS_SWAGGER_MODEL_NAME = "ExecutionProgressInfo";
}
