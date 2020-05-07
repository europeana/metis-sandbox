package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  private ProgressInfoDto progress;

  @JsonProperty("errors-report")
  private List<ReportByStepDto> errorsReport;

  public DatasetInfoDto(ProgressInfoDto progress, List<ReportByStepDto> errorsReport) {
    this.progress = progress;
    this.errorsReport = errorsReport;
  }

  public List<ReportByStepDto> getErrorsReport() {
    return errorsReport;
  }

  public void setErrorsReport(List<ReportByStepDto> errorsReport) {
    this.errorsReport = errorsReport;
  }

  public ProgressInfoDto getProgress() {
    return progress;
  }

  public void setProgress(ProgressInfoDto progress) {
    this.progress = progress;
  }
}
