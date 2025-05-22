package eu.europeana.metis.sandbox.batch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public final class FailExecutionRecordDTO extends ExecutionRecordDTO {

  @NotNull
  private final Exception exception;
}
