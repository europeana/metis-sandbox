package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.report.ExecutionProgressInfoDTO;
import eu.europeana.metis.sandbox.dto.report.ExecutionStatus;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;

/**
 * Represents a dataset along with its execution progress summary.
 * <p>
 * This class serves as a wrapper to combine dataset metadata and its corresponding execution progress in one unified structure
 * for API communication or internal data processing.
 */
@ApiModel(DatasetWithExecutionProgressSummaryDTO.SWAGGER_MODEL_NAME)
public record DatasetWithExecutionProgressSummaryDTO(
    @JsonProperty("dataset-id")
    String datasetId,

    @JsonProperty("dataset-name")
    String datasetName,

    @JsonProperty("created-by-id")
    String createdById,

    @JsonProperty("creation-date")
    ZonedDateTime creationDate,

    @JsonProperty("language")
    Language language,

    @JsonProperty("country")
    Country country,

    @JsonProperty("harvest-protocol")
    HarvestProtocol harvestProtocol,

    @JsonProperty("status")
    ExecutionStatus executionStatus,

    @JsonProperty("total-records")
    long totalRecords,

    @JsonProperty("processed-records")
    long processedRecords
) {

  public static final String SWAGGER_MODEL_NAME = "DatasetWithExecutionSummary";

  /**
   * Creates a new instance of DatasetWithExecutionProgressSummaryDTO by combining
   * the details from a DatasetInfoDTO and an ExecutionProgressInfoDTO.
   *
   * @param datasetInfoDTO the DTO containing metadata about the dataset.
   * @param executionProgressInfoDTO the DTO containing information about the execution progress of the dataset.
   * @return a new DatasetWithExecutionProgressSummaryDTO object that consolidates dataset metadata
   *         and execution progress details into a single structure.
   */
  public static DatasetWithExecutionProgressSummaryDTO from(
      DatasetInfoDTO datasetInfoDTO,
      ExecutionProgressInfoDTO executionProgressInfoDTO
  ) {
    return new DatasetWithExecutionProgressSummaryDTO(
        datasetInfoDTO.getDatasetId(),
        datasetInfoDTO.getDatasetName(),
        datasetInfoDTO.getCreatedById(),
        datasetInfoDTO.getCreationDate(),
        datasetInfoDTO.getLanguage(),
        datasetInfoDTO.getCountry(),
        datasetInfoDTO.getAbstractHarvestParametersDTO().getHarvestProtocol(),
        executionProgressInfoDTO.executionStatus(),
        executionProgressInfoDTO.totalRecords(),
        executionProgressInfoDTO.processedRecords()
    );
  }
}
