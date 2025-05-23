package eu.europeana.metis.sandbox.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobMetadataDTO {
  private final SuccessExecutionRecordDTO successExecutionRecordDTO;
  private final String targetExecutionName;
  private final String targetExecutionId;

}
