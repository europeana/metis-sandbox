package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes transformed to edm external events and performs external validation to the contained record <br/>
 * Publishes the result in the externally validated queue
 */
@Component
class TransformedToEdmExternalConsumer extends StepConsumer {

  private final ExternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String routingKey;

  public TransformedToEdmExternalConsumer(AmqpTemplate amqpTemplate,
      ExternalValidationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformation.edm.external.queue}", containerFactory = "...Factory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformation.edm.external.auto-start:true}")
  public void validateExternal(Event input) {
    consume(routingKey, input, Step.VALIDATE_EXTERNAL, () -> service.validate(input.getBody()));
  }

}
