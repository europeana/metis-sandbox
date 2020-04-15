package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.MediaProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes enriched events and performs media processing to the contained record <br/> Publishes
 * the result in the media processed queue
 */
@Component
class EnrichedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnrichedConsumer.class);

  private final AmqpTemplate amqpTemplate;
  private final MediaProcessingService service;

  @Value("${sandbox.rabbitmq.queues.record.media.queue}")
  private String routingKey;

  public EnrichedConsumer(AmqpTemplate amqpTemplate,
      MediaProcessingService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.enriched.queue}", containerFactory = "enrichedFactory")
  public void processMedia(Event input) {
//    if (input.getStatus() == Status.FAIL) {
//      return;
//    }

    Event output;
    Record record;
    try {
      record = service.processMedia(input.getBody());
      output = new Event(record, Step.MEDIA_PROCESS);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      record = Record.from(input.getBody(), input.getBody().getContent());
      output = new Event(record, Step.MEDIA_PROCESS, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
