package eu.europeana.metis.sandbox.dto;

import eu.europeana.metis.sandbox.dto.report.ReportByStepDto;
import io.swagger.annotations.ApiModel;
import java.util.List;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  private String progress;
  private List<ReportByStepDto> report;

  public DatasetInfoDto(String progress, List<ReportByStepDto> report) {
    this.progress = progress;
    this.report = report;
  }

  public List<ReportByStepDto> getReport() {
    return report;
  }

  public void setReport(List<ReportByStepDto> report) {
    this.report = report;
  }

  public String getProgress() {
    return progress;
  }

  public void setProgress(String progress) {
    this.progress = progress;
  }
}
