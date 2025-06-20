package eu.europeana.metis.sandbox.batch.dto;

import eu.europeana.indexing.tiers.model.TierResults;
import eu.europeana.metis.sandbox.common.ValidateObjectHelper;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Data Transfer Object representing a successful execution record with additional details.
 */
@Getter
@SuperBuilder(builderMethodName = "internalBuilder")
public final class SuccessExecutionRecordDTO extends AbstractExecutionRecordDTO {

  @NotBlank
  private final String recordData;
  @Builder.Default
  private final Set<Exception> exceptionWarnings = Set.of();
  private final TierResults tierResults;

  private static SuccessExecutionRecordDTOBuilder<?, ?> internalBuilder() {
    return new SuccessExecutionRecordDTOBuilderImpl();
  }

  /**
   * Creates and validates a new instance of SuccessExecutionRecordDTO using the provided builder configuration.
   *
   * @param builderSetup Consumer for configuring the SuccessExecutionRecordDTOBuilder.
   * @return A validated instance of SuccessExecutionRecordDTO.
   */
  public static SuccessExecutionRecordDTO createValidated(Consumer<SuccessExecutionRecordDTOBuilder<?, ?>> builderSetup) {
    return ValidateObjectHelper.buildValidated(
        SuccessExecutionRecordDTO::internalBuilder,
        SuccessExecutionRecordDTOBuilder::build, builderSetup
    );
  }

  /**
   * Creates a validated copy of the given SuccessExecutionRecordDTO with updated execution identifiers.
   *
   * @param sourceSuccessExecutionRecordDTO The source SuccessExecutionRecordDTO to copy data from.
   * @param executionId The new execution ID to be applied.
   * @param executionName The new execution name to be applied.
   * @param builderSetup A consumer that allows additional configuration of the SuccessExecutionRecordDTOBuilder.
   * @return A validated new instance of SuccessExecutionRecordDTO with updated identifiers.
   */
  public static SuccessExecutionRecordDTO createCopyIdentifiersValidated(SuccessExecutionRecordDTO sourceSuccessExecutionRecordDTO,
      String executionId, String executionName, Consumer<SuccessExecutionRecordDTOBuilder<?, ?>> builderSetup) {
    return ValidateObjectHelper.buildValidated(
        SuccessExecutionRecordDTO::internalBuilder,
        SuccessExecutionRecordDTOBuilder::build, builder -> {
          builder.datasetId(sourceSuccessExecutionRecordDTO.getDatasetId());
          builder.sourceRecordId(sourceSuccessExecutionRecordDTO.getSourceRecordId());
          builder.recordId(sourceSuccessExecutionRecordDTO.getRecordId());
          builder.executionId(executionId);
          builder.executionName(executionName);
        },
        builderSetup
    );
  }
}
