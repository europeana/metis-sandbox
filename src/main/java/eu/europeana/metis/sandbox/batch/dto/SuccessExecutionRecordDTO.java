package eu.europeana.metis.sandbox.batch.dto;

import eu.europeana.indexing.tiers.model.TierResults;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class SuccessExecutionRecordDTO extends ExecutionRecordDTO {

  @NotBlank
  private final String recordData;
  @Builder.Default
  private final Set<Exception> exceptionWarnings = Set.of();
  private final TierResults tierResults;

  public SuccessExecutionRecordDTOBuilder<?, ?> toBuilderOnlyIdentifiers(String executionId, String executionName) {
    return SuccessExecutionRecordDTO.builder()
                                    .datasetId(this.getDatasetId())
                                    .recordId(this.getRecordId())
                                    .executionId(executionId)
                                    .executionName(executionName);
  }
}
