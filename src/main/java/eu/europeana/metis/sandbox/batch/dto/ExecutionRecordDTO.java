package eu.europeana.metis.sandbox.batch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(builderMethodName = "internalBuilder")
public abstract sealed class ExecutionRecordDTO permits SuccessExecutionRecordDTO, FailExecutionRecordDTO {

  @NotBlank
  private String datasetId;
  @NotBlank
  private String sourceRecordId;
  @NotBlank
  private String recordId;
  @NotBlank
  private String executionId;
  @NotBlank
  private String executionName;
}
