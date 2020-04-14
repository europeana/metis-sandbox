package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes externally validated events and performs transformation to the contained record <br/>
 * Publishes the result in the transformed queue
 */
@Component
class ExternallyValidatedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternallyValidatedConsumer.class);

  private final AmqpTemplate amqpTemplate;
  private final TransformationService service;

  @Value("${sandbox.rabbitmq.queues.record.transformed.queue}")
  private String routingKey;

  public ExternallyValidatedConsumer(AmqpTemplate amqpTemplate,
      TransformationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.validated.external.queue}", containerFactory = "externallyValidatedFactory")
  public void transform(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    Record record;
    try {
      record = service.transform(input.getBody());
      output = new Event(record, Step.TRANSFORM);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      record = Record.from(input.getBody(), input.getBody().getContent());
      output = new Event(record, Step.TRANSFORM, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
