package eu.europeana.metis.sandbox.common;

import static eu.europeana.metis.sandbox.common.ValidateObjectHelper.validate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a request to create a dataset with the provided metadata.
 */
@Getter
@Builder
public class DatasetMetadataRequest {

  @NotBlank
  String datasetName;

  @NotNull
  Country country;

  @NotNull
  Language language;

  //todo: check if we keep this, use @Jacksonized or create a separate controller class
  @JsonCreator
  public DatasetMetadataRequest(
      @JsonProperty("datasetName") String datasetName,
      @JsonProperty("country") Country country,
      @JsonProperty("language") Language language) {
    this.datasetName = datasetName;
    this.country = country;
    this.language = language;
    validate(this);
  }
}
