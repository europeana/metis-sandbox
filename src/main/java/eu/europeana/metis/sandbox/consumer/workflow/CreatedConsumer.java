package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes created events and performs external validation to the contained record <br/> Publishes
 * the result in the externally validated queue
 */
@Component
class CreatedConsumer extends AmqpConsumer {

  private ExternalValidationService service;

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

    Event output = processEvent(input, Step.VALIDATE_EXTERNAL,
        () -> service.validate(input.getBody()));
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
