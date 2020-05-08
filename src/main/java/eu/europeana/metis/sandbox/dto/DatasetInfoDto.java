package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import eu.europeana.metis.sandbox.dto.report.StepErrorsDto;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  private ProgressInfoDto progress;

  @JsonProperty("errors-report")
  private List<StepErrorsDto> errorsReport;

  public DatasetInfoDto(ProgressInfoDto progress, List<StepErrorsDto> errorsReport) {
    requireNonNull(progress, "Progress must not be null");
    requireNonNull(errorsReport, "Errors report must not be null");
    this.progress = progress;
    this.errorsReport = errorsReport;
  }

  public List<StepErrorsDto> getErrorsReport() {
    return errorsReport;
  }

  public void setErrorsReport(List<StepErrorsDto> errorsReport) {
    this.errorsReport = errorsReport;
  }

  public ProgressInfoDto getProgress() {
    return progress;
  }

  public void setProgress(ProgressInfoDto progress) {
    this.progress = progress;
  }
}
