package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.NormalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes internally validated events and performs normalization to the contained record <br/>
 * Publishes the result in the normalized queue
 */
@Component
class InternallyValidatedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternallyValidatedConsumer.class);

  private final AmqpTemplate amqpTemplate;
  private final NormalizationService service;

  @Value("${sandbox.rabbitmq.queues.record.normalized.queue}")
  private String routingKey;

  public InternallyValidatedConsumer(AmqpTemplate amqpTemplate,
      NormalizationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.validated.internal.queue}", containerFactory = "internallyValidatedFactory")
  public void normalize(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    Record record;
    try {
      record = service.normalize(input.getBody());
      output = new Event(record, Step.NORMALIZE);
    } catch (RecordProcessingException ex) {
      LOGGER.error("Exception while performing normalization step", ex);
      output = new Event(input.getBody(), Step.NORMALIZE, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
