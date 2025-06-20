package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.dto.harvest.AbstractHarvestParametersDTO;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Getter;


/**
 * Represents information about a dataset.
 */
@ApiModel(DatasetInfoDTO.SWAGGER_MODEL_NAME)
@Getter
@Builder
public final class DatasetInfoDTO {

  public static final String SWAGGER_MODEL_NAME = "DatasetInfo";

  @JsonProperty("dataset-id")
  private final String datasetId;

  @JsonProperty("dataset-name")
  private final String datasetName;

  @JsonProperty("created-by-id")
  private final String createdById;

  @JsonProperty("creation-date")
  private final ZonedDateTime creationDate;

  @JsonProperty("language")
  private final Language language;

  @JsonProperty("country")
  private final Country country;

  @JsonProperty("transformed-to-edm-external")
  private final boolean transformedToEdmExternal;

  @JsonProperty("harvesting-parameters")
  private final AbstractHarvestParametersDTO abstractHarvestParametersDTO;
}
