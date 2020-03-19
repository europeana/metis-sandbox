package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class CreatedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CreatedConsumer.class);

  private AmqpTemplate amqpTemplate;
  private ExternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String routingKey;

  public CreatedConsumer(AmqpTemplate amqpTemplate,
      ExternalValidationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.created.queue}", containerFactory = "createdFactory")
  public void validateExternal(Event<Record> input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event<Record> output;
    Record record;
    try {
      record = service.validate(input.getBody());
      output = new Event<>(record, Step.VALIDATE_EXTERNAL);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      record = Record.from(input.getBody(), input.getBody().getContent());
      output = new Event<>(record, Step.VALIDATE_EXTERNAL, ex);
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
