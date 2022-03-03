package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;

import java.util.concurrent.CompletableFuture;

public interface AsyncRecordPublishService {

  /**
   * Async publish to message broker for further processing. This will send messages to 'created`
   * queue to skip the transformation to edm external step
   *
   * @param recordToPublish to publish. Must not be null
   * @param step The step that was performed
   * @return {@link CompletableFuture} of the process
   * @throws NullPointerException     if dataset object is null
   * @throws IllegalArgumentException if dataset records are empty
   */
  CompletableFuture<Void> publishWithoutXslt(RecordInfo recordToPublish, Step step);

  /**
   * Async publish to message broker for further processing. This will send messages to
   * 'transformationToEdmExternal' queue to go through the transformation to edm external step
   *
   * @param recordToPublish to publish. Must not be null
   * @param step The step that was performed
   * @return {@link CompletableFuture} of the process
   * @throws NullPointerException     if dataset object is null
   * @throws IllegalArgumentException if dataset records are empty
   */
  CompletableFuture<Void> publishWithXslt(RecordInfo recordToPublish, Step step);

}
