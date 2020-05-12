package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.dto.report.ProgressInfoDto;
import io.swagger.annotations.ApiModel;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  private final ProgressInfoDto progress;

  public DatasetInfoDto(ProgressInfoDto progress) {
    requireNonNull(progress, "Progress must not be null");
    this.progress = progress;
  }

  public ProgressInfoDto getProgress() {
    return progress;
  }
}
