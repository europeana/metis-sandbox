package eu.europeana.metis.sandbox.dto.debias;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.service.debias.DeBiasProcessService.DeBiasReportRow;
import io.swagger.annotations.ApiModel;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

/**
 * The type Detection info dto.
 */
@ApiModel(DeBiasReportDTO.SWAGGER_MODEL_NAME)
public class DeBiasReportDTO extends DeBiasStatusDTO {

  /**
   * The constant SWAGGER_MODEL_NAME.
   */
  public static final String SWAGGER_MODEL_NAME = "DeBiasReportDto";

  @JsonProperty("detections")
  private final List<DeBiasReportRow> deBiasReportRowList;

  /**
   * Instantiates a new Detection info dto.
   *
   * @param datasetId the dataset id
   * @param debiasState the state
   * @param creationDate the creation date
   * @param total the total records
   * @param processed the processed records
   * @param deBiasReportRowList the de bias report row list
   */
  public DeBiasReportDTO(Integer datasetId, DebiasState debiasState, ZonedDateTime creationDate, long total, long processed, List<DeBiasReportRow> deBiasReportRowList) {
    super(datasetId, debiasState, creationDate, total, processed);
    this.deBiasReportRowList = Collections.unmodifiableList(deBiasReportRowList);
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
