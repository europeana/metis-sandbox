package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel(ExecutionProgressInfoDTO.PROGRESS_SWAGGER_MODEL_NAME)
public record ExecutionProgressInfoDTO(
    @JsonProperty("portal-publish")
    String portalPublishUrl,

    @JsonProperty("status")
    ExecutionStatus executionStatus,

    @JsonProperty("total-records")
    long totalRecords,

    @JsonProperty("processed-records")
    long processedRecords,

    @JsonProperty("progress-by-step")
    List<ExecutionProgressByStepDTO> executionProgressByStepDTOS,

    @JsonProperty("record-limit-reached")
    boolean recordLimitReached,

    @JsonProperty("tier-zero-info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    TiersZeroInfoDTO tiersZeroInfoDTO
) {

  public static final String PROGRESS_SWAGGER_MODEL_NAME = "ExecutionProgressInfo";
}
