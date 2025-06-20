package eu.europeana.metis.sandbox.common;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import lombok.NonNull;

/**
 * Represents a request to create a dataset with the provided metadata.
 */
public record DatasetMetadataRequest(@NonNull String datasetName, @NonNull Country country, @NonNull Language language) {

}
