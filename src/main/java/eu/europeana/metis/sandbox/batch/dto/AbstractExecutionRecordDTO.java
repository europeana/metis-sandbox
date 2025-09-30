package eu.europeana.metis.sandbox.batch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Abstract Data Transfer Object representing the base structure of an execution record.
 *
 * <p>This class is designed to be extended by specific implementations that represent either
 * a successful execution or a failure scenario.
 * <p>It contains common properties that uniquely identify the context and execution process
 * related to a dataset.
 */
@Getter
@SuperBuilder(builderMethodName = "internalBuilder")
@SuppressWarnings({
    "javaarchitecture:S7027", // False positive circular detection. Valid sealed/permits mapping.
    "java:S1694"              // Abstract DTO for polymorphic structure – no abstract methods needed.
})
public abstract sealed class AbstractExecutionRecordDTO permits SuccessExecutionRecordDTO, FailExecutionRecordDTO {

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
