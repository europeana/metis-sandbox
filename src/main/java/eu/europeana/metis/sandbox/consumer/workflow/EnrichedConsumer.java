package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.MediaProcessingService;
import java.util.List;
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
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    try {
      var record = service.processMedia(input.getBody());
      var status = record.getErrors().isEmpty() ? Status.SUCCESS : Status.FAIL_NON_STOP;
      output = new Event(record, Step.MEDIA_PROCESS, status);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      var recordError = new RecordError(ex);
      output = new Event(new RecordInfo(input.getBody(), List.of(recordError)), Step.MEDIA_PROCESS,
          Status.FAIL);
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
