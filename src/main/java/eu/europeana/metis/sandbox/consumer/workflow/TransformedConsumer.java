package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes transformed events and performs internal validation to the contained record <br/>
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
    try {
      var record = service.validate(input.getBody());
      var status = record.getErrors().isEmpty() ? Status.SUCCESS : Status.FAIL_NON_STOP;
      output = new Event(record, Step.VALIDATE_INTERNAL, status);
    } catch (RecordProcessingException ex) {
      LOGGER.error(ex.getMessage(), ex);
      var recordError = new RecordError(ex);
      output = new Event(new RecordInfo(input.getBody(), List.of(recordError)),
          Step.VALIDATE_INTERNAL, Status.FAIL);
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
