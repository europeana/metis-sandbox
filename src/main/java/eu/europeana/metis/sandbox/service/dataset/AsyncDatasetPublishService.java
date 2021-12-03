package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.domain.Dataset;
import java.util.concurrent.CompletableFuture;

public interface AsyncDatasetPublishService {

  /**
   * Async publish to message broker for further processing.
   *
   * @param dataset to publish. Must not be null
   * @return {@link CompletableFuture} of the process
   * @throws NullPointerException     if dataset object is null
   * @throws IllegalArgumentException if dataset records are empty
   */
  CompletableFuture<Void> publish(Dataset dataset, boolean hasXsltToEdmExternal);
}
