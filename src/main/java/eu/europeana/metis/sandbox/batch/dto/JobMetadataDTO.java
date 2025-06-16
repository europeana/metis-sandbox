package eu.europeana.metis.sandbox.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Data Transfer Object that contains metadata for a job execution.
 */
@Getter
@AllArgsConstructor
public class JobMetadataDTO {

  private final SuccessExecutionRecordDTO successExecutionRecordDTO;
  private final String targetExecutionName;
  private final String targetExecutionId;

}
