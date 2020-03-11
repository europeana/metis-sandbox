package eu.europeana.metis.sandbox.dto;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ApiModel("Dataset")
@RequiredArgsConstructor
@ToString
@Getter
public class DatasetIdDto {

  @NonNull
  private final String datasetId;
}
