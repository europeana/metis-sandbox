package eu.europeana.metis.sandbox.dto;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.StringJoiner;

@ApiModel("DatasetInfo")
public class DatasetInfoDto {

  @JsonProperty("processed")
  private final Integer recordsProcessed;

  @JsonProperty("success")
  private final Integer recordsSucceeded;

  @JsonProperty("fail")
  private final Integer recordsFailed;

  @JsonProperty("pending")
  private final Integer recordsPending;

  @JsonProperty("fails")
  private final List<String> recordsFailedList;

  public DatasetInfoDto(Integer recordsProcessed, Integer recordsSucceeded,
      Integer recordsFailed, Integer recordsPending,
      List<String> recordsFailedList) {
    requireNonNull(recordsProcessed, "Records processed must not be null");
    requireNonNull(recordsSucceeded, "Records succeeded must not be null");
    requireNonNull(recordsFailed, "Records failed must not be null");
    requireNonNull(recordsPending, "Records pending must not be null");
    requireNonNull(recordsFailedList, "Records failed list must not be null");
    this.recordsProcessed = recordsProcessed;
    this.recordsSucceeded = recordsSucceeded;
    this.recordsFailed = recordsFailed;
    this.recordsPending = recordsPending;
    this.recordsFailedList = recordsFailedList;
  }

  public Integer getRecordsProcessed() {
    return this.recordsProcessed;
  }

  public Integer getRecordsSucceeded() {
    return this.recordsSucceeded;
  }

  public Integer getRecordsFailed() {
    return this.recordsFailed;
  }

  public Integer getRecordsPending() {
    return this.recordsPending;
  }

  public List<String> getRecordsFailedList() {
    return this.recordsFailedList;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DatasetInfoDto.class.getSimpleName() + "[", "]")
        .add("recordsProcessed=" + recordsProcessed)
        .add("recordsSucceeded=" + recordsSucceeded)
        .add("recordsFailed=" + recordsFailed)
        .add("recordsPending=" + recordsPending)
        .add("recordsFailedList=" + recordsFailedList)
        .toString();
  }
}
