package eu.europeana.metis.sandbox.batch.dto;

import jakarta.validation.constraints.NotNull;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Represents an execution record that indicates a failure and contains details about the exception. This class is a final
 * implementation of the abstract {@link ExecutionRecordDTO}.
 */
@Getter
@SuperBuilder(builderMethodName = "internalBuilder")
public final class FailExecutionRecordDTO extends ExecutionRecordDTO {

  @NotNull
  private final Exception exception;

  /**
   * Creates a validated instance of FailExecutionRecordDTO using the provided configuration.
   *
   * @param builderSetup A consumer to configure the FailExecutionRecordDTOBuilder.
   * @return A validated FailExecutionRecordDTO instance.
   */
  public static FailExecutionRecordDTO createValidated(Consumer<FailExecutionRecordDTOBuilder<?, ?>> builderSetup) {
    return ValidatedBuilderUtil.buildValidated(
        FailExecutionRecordDTO::internalBuilder,
        FailExecutionRecordDTOBuilder::build, builderSetup
    );
  }
}
