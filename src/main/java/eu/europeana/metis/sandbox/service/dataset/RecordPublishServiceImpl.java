package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
class RecordPublishServiceImpl implements RecordPublishService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      RecordPublishServiceImpl.class);

  private final AmqpTemplate amqpTemplate;
  private final String createdQueue;
  private final String transformationToEdmExternalQueue;

  public RecordPublishServiceImpl(AmqpTemplate amqpTemplate, String createdQueue, String transformationToEdmExternalQueue) {
    this.amqpTemplate = amqpTemplate;
    this.createdQueue = createdQueue;
    this.transformationToEdmExternalQueue = transformationToEdmExternalQueue;
  }

  @Override
  public void publishToHarvestQueue(RecordInfo recordInfo, Step step) {
    try {
      amqpTemplate.convertAndSend(createdQueue,
          new RecordProcessEvent(recordInfo, step, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecord().getRecordId(), e);
    }
  }

  @Override
  public void publishToTransformationToEdmExternalQueue(RecordInfo recordInfo, Step step) {
    try {
      amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
          new RecordProcessEvent(recordInfo, step, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecord().getRecordId(), e);
    }
  }

}
