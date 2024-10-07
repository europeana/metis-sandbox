package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessServiceImpl.DeBiasReportRow;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

/**
 * The type Detection info dto.
 */
@ApiModel(DeBiasReportDto.SWAGGER_MODEL_NAME)
public class DeBiasReportDto {

  /**
   * The constant SWAGGER_MODEL_NAME.
   */
  public static final String SWAGGER_MODEL_NAME = "DebiasDetectionInfo";

  @JsonProperty("dataset-id")
  private final Integer datasetId;

  @JsonProperty("state")
  private final String state;

  @JsonProperty("creation-date")
  @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
  private final ZonedDateTime creationDate;

  @JsonProperty("detections")
  private final List<DeBiasReportRow> deBiasReportRowList;

  /**
   * Instantiates a new Detection info dto.
   *
   * @param datasetId the dataset id
   * @param state the state
   * @param creationDate the creation date
   * @param deBiasReportRowList the de bias report row list
   */
  public DeBiasReportDto(Integer datasetId, String state, ZonedDateTime creationDate,  List<DeBiasReportRow> deBiasReportRowList) {
    this.datasetId = datasetId;
    this.state = state;
    this.creationDate = creationDate;
    this.deBiasReportRowList = deBiasReportRowList;
  }

  /**
   * Gets dataset id.
   *
   * @return the dataset id
   */
  public Integer getDatasetId() {
    return datasetId;
  }

  /**
   * Gets state.
   *
   * @return the state
   */
  public String getState() {
    return state;
  }

  /**
   * Gets creation date.
   *
   * @return the creation date
   */
  public ZonedDateTime getCreationDate() {
    return creationDate;
  }

  /**
   * Gets DeBias report row list.
   *
   * @return the DeBias report row list
   */
  public List<DeBiasReportRow> getDeBiasReportRowList() {
    return Collections.unmodifiableList(deBiasReportRowList);
  }
}
