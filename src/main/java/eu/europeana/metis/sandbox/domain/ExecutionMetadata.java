package eu.europeana.metis.sandbox.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class ExecutionMetadata {

  @NonNull
  private final DatasetMetadata datasetMetadata;

  private final InputMetadata inputMetadata;

}
