package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.batch.common.FullBatchJobType;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

/**
 * Base of the dataset report
 */
@ApiModel(ProgressInfoDTO.PROGRESS_SWAGGER_MODEL_NAME)
public class ProgressInfoDTO {

    public static final String PROGRESS_SWAGGER_MODEL_NAME = "ProgressInfo";

    @JsonProperty("portal-publish")
    private final String portalPublishUrl;

    private final Status status;

    @JsonProperty("total-records")
    private final Long totalRecords;

    @JsonProperty("processed-records")
    private final Long processedRecords;

    @JsonProperty("progress-by-step")
    private final List<ProgressByStepDTO> progressByStep;

    @JsonProperty("record-limit-exceeded")
    private final boolean recordLimitExceeded;

    @JsonProperty("error-type")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String errorType;

    @JsonProperty("tier-zero-info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final TiersZeroInfoDTO tiersZeroInfoDTO;

    @JsonProperty("dataset-logs")
    private final List<DatasetLogDTO> datasetLogs;

    @JsonProperty("records-published-successfully")
    private final boolean recordsPublishedSuccessfully;

    public ProgressInfoDTO(String portalPublishUrl, Long totalRecords, Long processedRecords,
                           List<ProgressByStepDTO> progressByStep, boolean recordLimitExceeded, String errorType,
                           List<DatasetLogDTO> datasetLogs, TiersZeroInfoDTO tiersZeroInfoDTO) {
        this.processedRecords = processedRecords;
        this.tiersZeroInfoDTO = tiersZeroInfoDTO;
        if (!errorType.isEmpty()) {
            this.status = Status.FAILED;
            this.totalRecords = totalRecords != null ? totalRecords : 0L;
        } else if (totalRecords == null) {
            this.status = Status.HARVESTING_IDENTIFIERS;
            this.totalRecords = 0L;
        } else if (totalRecords.equals(this.processedRecords)) {
            this.status = Status.COMPLETED;
            this.totalRecords = totalRecords;
        } else {
            this.status = Status.IN_PROGRESS;
            this.totalRecords = totalRecords;
        }
        this.datasetLogs = datasetLogs;
        this.progressByStep = progressByStep.stream().filter(s -> s.step()!= FullBatchJobType.DEBIAS).toList();
        this.recordLimitExceeded = recordLimitExceeded;
        this.errorType = errorType;
        this.recordsPublishedSuccessfully =
                progressByStep.stream().filter(step -> step.step() == FullBatchJobType.INDEX).findAny()
                        .map(step -> step.success() + step.warn() > 0).orElse(false);
        this.portalPublishUrl = this.recordsPublishedSuccessfully ? portalPublishUrl : "";
    }

    public String getPortalPublishUrl() {
        return portalPublishUrl;
    }

    public Status getStatus() {
        return status;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public long getProcessedRecords() {
        return processedRecords;
    }

    public List<ProgressByStepDTO> getProgressByStep() {
        return Collections.unmodifiableList(progressByStep);
    }

    public boolean getRecordLimitExceeded() {
        return recordLimitExceeded;
    }

    public String getErrorType() {
        return errorType;
    }

    public TiersZeroInfoDTO getTiersZeroInfo() {
        return tiersZeroInfoDTO;
    }

    public List<DatasetLogDTO> getDatasetLogs() {
        return datasetLogs;
    }

    public boolean isRecordsPublishedSuccessfully() {
        return recordsPublishedSuccessfully;
    }

    public enum Status {
        HARVESTING_IDENTIFIERS("Harvesting Identifiers"),
        COMPLETED("Completed"),
        IN_PROGRESS("In Progress"),
        FAILED("Failed");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
