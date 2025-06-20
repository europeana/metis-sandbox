package eu.europeana.metis.sandbox.common;

import static eu.europeana.metis.sandbox.common.ValidateObjectHelper.validate;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a request to create a dataset with the provided metadata.
 */
public record DatasetMetadataRequest(@NotBlank String datasetName, @NotNull Country country, @NotNull Language language) {

  public DatasetMetadataRequest(String datasetName, Country country, Language language) {
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    validate(this);
  }
}
