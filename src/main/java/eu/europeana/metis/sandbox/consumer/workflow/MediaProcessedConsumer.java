package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.IndexEnv;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes media processed events and performs indexing to the contained record <br/> Publishes the
 * result in the previewed queue
 */
@Component
class MediaProcessedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(MediaProcessedConsumer.class);

  private final AmqpTemplate amqpTemplate;
  private final IndexingService service;

  @Value("${sandbox.rabbitmq.queues.record.previewed.queue}")
  private String routingKey;

  public MediaProcessedConsumer(AmqpTemplate amqpTemplate,
      IndexingService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.media.queue}", containerFactory = "mediaProcessedFactory")
  public void index(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    try {
      var record = service.index(input.getBody(), IndexEnv.PREVIEW);
      var status = record.getErrors().isEmpty() ? Status.SUCCESS : Status.WARN;
      output = new Event(record, Step.PREVIEW, status);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      var recordError = new RecordError(ex);
      output = new Event(new RecordInfo(input.getBody(), List.of(recordError)), Step.PREVIEW,
          Status.FAIL);
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
