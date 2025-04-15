package eu.europeana.metis.sandbox.service.dataset;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
class RecordPublishServiceImpl implements RecordPublishService {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final AmqpTemplate amqpTemplate;
  private final String createdQueue;
  private final String transformationToEdmExternalQueue;

  public RecordPublishServiceImpl(
      AmqpTemplate amqpTemplate,
      @Qualifier("createdQueue") String createdQueue,
      @Qualifier("transformationToEdmExternalQueue") String transformationToEdmExternalQueue) {
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
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecordValue().getRecordId(), e);
    }
  }

  @Override
  public void publishToTransformationToEdmExternalQueue(RecordInfo recordInfo, Step step) {
    try {
      amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
          new RecordProcessEvent(recordInfo, step, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecordValue().getRecordId(), e);
    }
  }

}
