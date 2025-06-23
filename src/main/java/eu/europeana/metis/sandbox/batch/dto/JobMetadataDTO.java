package eu.europeana.metis.sandbox.batch.dto;

import static eu.europeana.metis.sandbox.common.ValidateObjectHelper.validate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Data Transfer Object that contains metadata for a job execution.
 */
@Value
@Builder
public class JobMetadataDTO {

  @NotNull
  SuccessExecutionRecordDTO successExecutionRecordDTO;

  @NotBlank
  String targetExecutionName;

  @NotBlank
  String targetExecutionId;

  private JobMetadataDTO(SuccessExecutionRecordDTO successExecutionRecordDTO, String targetExecutionName,
      String targetExecutionId) {
    this.successExecutionRecordDTO = successExecutionRecordDTO;
    this.targetExecutionName = targetExecutionName;
    this.targetExecutionId = targetExecutionId;
    validate(this);
  }
}
