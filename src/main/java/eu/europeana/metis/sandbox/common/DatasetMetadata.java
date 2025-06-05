package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@Getter
@AllArgsConstructor
public final class DatasetMetadata {

  @NonNull
  private final String datasetId;

  @NonNull
  private final String datasetName;

  @NonNull
  private final Country country;

  @NonNull
  private final Language language;

  @NonNull
  private final Integer stepSize;

  @NonNull
  private final WorkflowType workflowType;
}
