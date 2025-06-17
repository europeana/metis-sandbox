package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Immutable class representing metadata for a dataset.
 *
 * <p>Contains information about the dataset, including its identifier, name, associated country, language, and workflow type.
 * <p>All fields in this class are mandatory and must not be null.
 */
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
  private final WorkflowType workflowType;
}
