package eu.europeana.metis.sandbox.batch.dto;

import static eu.europeana.metis.sandbox.common.ValidateObjectHelper.validate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object that contains metadata for a job execution.
 */
public record JobMetadataDTO(@NotNull SuccessExecutionRecordDTO successExecutionRecordDTO, @NotBlank String targetExecutionName,
                             @NotBlank String targetExecutionId) {

  public JobMetadataDTO(@NotNull SuccessExecutionRecordDTO successExecutionRecordDTO, @NotBlank String targetExecutionName,
      @NotBlank String targetExecutionId) {
    this.successExecutionRecordDTO = successExecutionRecordDTO;
    this.targetExecutionName = targetExecutionName;
    this.targetExecutionId = targetExecutionId;
    validate(this);
  }
}
