package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import io.swagger.annotations.ApiModel;
import java.util.Collections;
import java.util.List;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  private final String progress;

  @JsonProperty("errors-report")
  private final List<ReportByStepDto> errorsReport;

  public DatasetInfoDto(String progress, List<ReportByStepDto> errorsReport) {
    this.progress = progress;
    this.errorsReport = Collections.unmodifiableList(errorsReport);
  }

  public List<ReportByStepDto> getErrorsReport() {
    return errorsReport;
  }

  public String getProgress() {
    return progress;
  }
}
