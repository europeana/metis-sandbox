package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes created events and performs external validation to the contained record <br/> Publishes
 * the result in the externally validated queue
 */
@Component
class CreatedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreatedConsumer.class);

  private final AmqpTemplate amqpTemplate;
  private final ExternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String routingKey;

  public CreatedConsumer(AmqpTemplate amqpTemplate,
      ExternalValidationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.created.queue}", containerFactory = "createdFactory")
  public void validateExternal(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    Record record;
    try {
      record = service.validate(input.getBody());
      output = new Event(record, Step.VALIDATE_EXTERNAL);
    } catch (RecordProcessingException ex) {
      LOGGER.error("Exception while performing external validation step", ex);
      output = new Event(input.getBody(), Step.VALIDATE_EXTERNAL, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
