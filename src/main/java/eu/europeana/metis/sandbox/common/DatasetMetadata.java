package eu.europeana.metis.sandbox.common;

import static eu.europeana.metis.sandbox.common.ValidateObjectHelper.validate;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.entity.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable class representing metadata for a dataset.
 *
 * <p>Contains information about the dataset, including its identifier, name, associated country, language, and workflow type.
 * <p>All fields in this class are mandatory and must not be null.
 */
@Value
@Builder
public class DatasetMetadata {

  @NotBlank
  String datasetId;

  @NotBlank
  String datasetName;

  @NotNull
  Country country;

  @NotNull
  Language language;

  @NotNull
  WorkflowType workflowType;

  private DatasetMetadata(String datasetId, String datasetName, Country country,
      Language language, WorkflowType workflowType) {
    this.datasetId = datasetId;
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    this.workflowType = workflowType;
    validate(this);
  }
}
