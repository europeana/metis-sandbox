package eu.europeana.metis.sandbox.dto.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.Step;
import io.swagger.annotations.ApiModel;

import java.util.Collections;
import java.util.List;

/**
 * Base of the dataset report
 */
@ApiModel(ProgressInfoDto.PROGRESS_SWAGGER_MODEL_NAME)
public class ProgressInfoDto {

    public static final String PROGRESS_SWAGGER_MODEL_NAME = "ProgressInfo";

    @JsonProperty("portal-publish")
    private final String portalPublishUrl;

    private final Status status;

    @JsonProperty("total-records")
    private final Long totalRecords;

    @JsonProperty("processed-records")
    private final Long processedRecords;

    @JsonProperty("progress-by-step")
    private final List<ProgressByStepDto> progressByStep;

    @JsonProperty("record-limit-exceeded")
    private final boolean recordLimitExceeded;

    @JsonProperty("error-type")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String errorType;

    @JsonProperty("tier-zero-info")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final TiersZeroInfo tiersZeroInfo;

    @JsonProperty("dataset-logs")
    private final List<DatasetLogDto> datasetLogs;

    @JsonProperty("records-published-successfully")
    private final boolean recordsPublishedSuccessfully;

    public ProgressInfoDto(String portalPublishUrl, Long totalRecords, Long processedRecords,
                           List<ProgressByStepDto> progressByStep, boolean recordLimitExceeded, String errorType,
                           List<DatasetLogDto> datasetLogs, TiersZeroInfo tiersZeroInfo) {
        this.processedRecords = processedRecords;
        this.tiersZeroInfo = tiersZeroInfo;
        if (!errorType.equals("")) {
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
        this.progressByStep = Collections.unmodifiableList(progressByStep);
        this.recordLimitExceeded = recordLimitExceeded;
        this.errorType = errorType;
        this.recordsPublishedSuccessfully =
                progressByStep.stream().filter(step -> step.getStep() == Step.PUBLISH).findAny()
                        .map(step -> step.getSuccess() + step.getWarn() > 0).orElse(false);
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

    public List<ProgressByStepDto> getProgressByStep() {
        return progressByStep;
    }

    public boolean getRecordLimitExceeded() {
        return recordLimitExceeded;
    }

    public String getErrorType() {
        return errorType;
    }

    public TiersZeroInfo getTiersZeroInfo() {
        return tiersZeroInfo;
    }

    public List<DatasetLogDto> getDatasetLogs() {
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
