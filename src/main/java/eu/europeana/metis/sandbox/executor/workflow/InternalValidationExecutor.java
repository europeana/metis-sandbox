package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes transformed events and performs internal validation to the contained record <br/>
 * Publishes the result in the internally validated queue
 */
@Component
class InternalValidationExecutor extends StepExecutor {

  private final InternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.queue}")
  private String routingKey;

  public InternalValidationExecutor(AmqpTemplate amqpTemplate,
      InternalValidationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformed.queue}",
      containerFactory = "internalValidationFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformed.auto-start:true}")
  public void validateInternal(RecordProcessEvent event) {
    consume(routingKey, event, Step.VALIDATE_INTERNAL, () -> service.validate(event.getRecord()));
  }
}
