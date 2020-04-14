package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes transformed events and performs internal validation to the contained record
 * <br/>
 * Publishes the result in the internally validated queue
 */
@Component
class TransformedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformedConsumer.class);

  private final AmqpTemplate amqpTemplate;
  private final InternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.queue}")
  private String routingKey;

  public TransformedConsumer(AmqpTemplate amqpTemplate,
      InternalValidationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformed.queue}", containerFactory = "transformedFactory")
  public void validateInternal(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    Record record;
    try {
      record = service.validate(input.getBody());
      output = new Event(record, Step.VALIDATE_INTERNAL);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      record = Record.from(input.getBody(), input.getBody().getContent());
      output = new Event(record, Step.VALIDATE_INTERNAL, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
