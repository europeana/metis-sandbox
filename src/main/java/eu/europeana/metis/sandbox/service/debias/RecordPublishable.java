package eu.europeana.metis.sandbox.service.debias;

import eu.europeana.metis.sandbox.domain.RecordInfo;

/**
 * The interface Record publishable.
 */
public interface RecordPublishable {

  /**
   * Publish to de bias queue.
   *
   * @param recordToPublish the record to publish
   */
  void publishToDeBiasQueue(RecordInfo recordToPublish);
}
