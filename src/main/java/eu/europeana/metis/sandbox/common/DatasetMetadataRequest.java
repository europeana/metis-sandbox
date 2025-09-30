package eu.europeana.metis.sandbox.common;

import static eu.europeana.metis.sandbox.common.ValidateObjectHelper.validate;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a request to create a dataset with the provided metadata.
 */
@Value
@Builder
public final class DatasetMetadataRequest {

  @NotBlank
  String datasetName;

  @NotNull
  Country country;

  @NotNull
  Language language;

  @SuppressWarnings("java:S1144") //False positive. It is valid and used by lombok builder to validate the object
  private DatasetMetadataRequest(String datasetName, Country country, Language language) {
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    validate(this);
  }
}
