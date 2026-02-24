package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.HarvestProtocol;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

/**
 * Represents a dataset summary.
 */
@Schema(name = DatasetSummaryDTO.SWAGGER_MODEL_NAME)
public record DatasetSummaryDTO(
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
    HarvestProtocol harvestProtocol
) {

  public static final String SWAGGER_MODEL_NAME = "DatasetWithExecutionSummary";

  /**
   * Creates a new instance of DatasetWithExecutionProgressSummaryDTO by combining the details from a DatasetInfoDTO and an
   * ExecutionProgressInfoDTO.
   *
   * @param datasetInfoDTO the DTO containing metadata about the dataset.
   * @return a new DatasetWithExecutionProgressSummaryDTO object that consolidates dataset metadata.
   */
  public static DatasetSummaryDTO from(DatasetInfoDTO datasetInfoDTO) {
    return new DatasetSummaryDTO(
        datasetInfoDTO.getDatasetId(),
        datasetInfoDTO.getDatasetName(),
        datasetInfoDTO.getCreatedById(),
        datasetInfoDTO.getCreationDate(),
        datasetInfoDTO.getLanguage(),
        datasetInfoDTO.getCountry(),
        datasetInfoDTO.getAbstractHarvestParametersDTO().getHarvestProtocol()
    );
  }
}
