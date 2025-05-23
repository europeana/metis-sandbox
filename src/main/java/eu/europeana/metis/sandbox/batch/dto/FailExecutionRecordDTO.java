package eu.europeana.metis.sandbox.batch.dto;

import jakarta.validation.constraints.NotNull;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder(builderMethodName = "internalBuilder")
public final class FailExecutionRecordDTO extends ExecutionRecordDTO {

  @NotNull
  private final Exception exception;

  public static FailExecutionRecordDTO createValidated(Consumer<FailExecutionRecordDTOBuilder<?, ?>> builderSetup) {
    return ValidatedBuilderUtil.buildValidated(
        FailExecutionRecordDTO::internalBuilder,
        FailExecutionRecordDTOBuilder::build, builderSetup
    );
  }
}
