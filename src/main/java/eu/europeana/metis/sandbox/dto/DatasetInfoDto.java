package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  private String progress;

  @JsonProperty("errors-report")
  private List<ReportByStepDto> errorsReport;

  public DatasetInfoDto(String progress, List<ReportByStepDto> errorsReport) {
    this.progress = progress;
    this.errorsReport = errorsReport;
  }

  public List<ReportByStepDto> getErrorsReport() {
    return errorsReport;
  }

  public void setErrorsReport(List<ReportByStepDto> errorsReport) {
    this.errorsReport = errorsReport;
  }

  public String getProgress() {
    return progress;
  }

  public void setProgress(String progress) {
    this.progress = progress;
  }
}
