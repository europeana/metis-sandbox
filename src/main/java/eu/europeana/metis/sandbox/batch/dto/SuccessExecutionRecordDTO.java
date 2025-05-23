package eu.europeana.metis.sandbox.batch.dto;

import eu.europeana.indexing.tiers.model.TierResults;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(builderMethodName = "internalBuilder")
public final class SuccessExecutionRecordDTO extends ExecutionRecordDTO {

  @NotBlank
  private final String recordData;
  @Builder.Default
  private final Set<Exception> exceptionWarnings = Set.of();
  private final TierResults tierResults;

  private static SuccessExecutionRecordDTOBuilder<?, ?> internalBuilder() {
    return new SuccessExecutionRecordDTOBuilderImpl();
  }

  public static SuccessExecutionRecordDTO createValidated(Consumer<SuccessExecutionRecordDTOBuilder<?, ?>> builderSetup) {
    return ValidatedBuilderUtil.buildValidated(
        SuccessExecutionRecordDTO::internalBuilder,
        SuccessExecutionRecordDTOBuilder::build, builderSetup
    );
  }

  public static SuccessExecutionRecordDTO createCopyIdentifiersValidated(SuccessExecutionRecordDTO sourceSuccessExecutionRecordDTO,
      String executionId, String executionName, Consumer<SuccessExecutionRecordDTOBuilder<?, ?>> builderSetup) {
    return ValidatedBuilderUtil.buildValidated(
        SuccessExecutionRecordDTO::internalBuilder,
        SuccessExecutionRecordDTOBuilder::build, builder -> {
          builder.datasetId(sourceSuccessExecutionRecordDTO.getDatasetId());
          builder.recordId(sourceSuccessExecutionRecordDTO.getRecordId());
          builder.executionId(executionId);
          builder.executionName(executionName);
        },
        builderSetup
    );
  }
}
