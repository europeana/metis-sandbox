package eu.europeana.metis.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ApiModel("DatasetInfo")
@RequiredArgsConstructor
@ToString
@Getter
public class DatasetInfoDto {

  @NonNull
  @JsonProperty("processed")
  private final  Integer recordsProcessed;

  @NonNull
  @JsonProperty("success")
  private final Integer recordsSucceeded;

  @NonNull
  @JsonProperty("fail")
  private final Integer recordsFailed;

  @NonNull
  @JsonProperty("pending")
  private final Integer recordsPending;

  @NonNull
  @JsonProperty("fails")
  private final List<String> recordsFailedList;

}
