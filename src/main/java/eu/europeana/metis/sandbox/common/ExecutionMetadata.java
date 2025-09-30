package eu.europeana.metis.sandbox.common;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Immutable class representing metadata related to the execution process.
 *
 * <p>Holds information about the dataset being processed through the {@link DatasetMetadata} field.
 * <p>Optionally, can include metadata about input parameters via the {@link InputMetadata} field.
 */
@Getter
@Builder
public class ExecutionMetadata {

  @NonNull
  private final DatasetMetadata datasetMetadata;

  private final InputMetadata inputMetadata;

}
