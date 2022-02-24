package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public interface AsyncDatasetPublishService {

  /**
   * Async publish to message broker for further processing. This will send messages to 'created`
   * queue to skip the transformation to edm external step
   *
   * @param dataset to publish. Must not be null
   * @return {@link CompletableFuture} of the process
   * @throws NullPointerException     if dataset object is null
   * @throws IllegalArgumentException if dataset records are empty
   */
  CompletableFuture<Void> publishWithoutXslt(Dataset dataset);

  /**
   * Async publish to message broker for further processing. This will send messages to
   * 'transformationToEdmExternal' queue to go through the transformation to edm external step
   *
   * @param dataset to publish. Must not be null
   * @return {@link CompletableFuture} of the process
   * @throws NullPointerException     if dataset object is null
   * @throws IllegalArgumentException if dataset records are empty
   */
  CompletableFuture<Void> publishWithXslt(Dataset dataset);

  void publishToCreatedQueue(Record record);

  CompletableFuture<Void> harvestOaiPmh(String datasetName, String datasetId, Country country, Language language,
                                        InputStream xsltInputStream,
                                        OaiHarvestData oaiHarvestData);
}
