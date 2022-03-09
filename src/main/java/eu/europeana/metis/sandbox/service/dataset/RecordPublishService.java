package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;

public interface RecordPublishService {

  /**
   * Publish to message broker for further processing. This will send messages to 'created`
   * queue to skip the transformation to edm external step
   *
   * @param recordToPublish to publish. Must not be null
   * @param step The step that was performed
   */
  void publishToHarvestQueue(RecordInfo recordToPublish, Step step);

  /**
   * Publish to message broker for further processing. This will send messages to
   * 'transformationToEdmExternal' queue to go through the transformation to edm external step
   *
   * @param recordToPublish to publish. Must not be null
   * @param step The step that was performed
   */
  void publishToTransformationToEdmExternalQueue(RecordInfo recordToPublish, Step step);

}
