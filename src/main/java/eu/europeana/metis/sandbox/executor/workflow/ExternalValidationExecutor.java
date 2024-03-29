package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
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
class ExternalValidationExecutor extends StepExecutor {

  private final ExternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String routingKey;

  public ExternalValidationExecutor(AmqpTemplate amqpTemplate,
      ExternalValidationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = {"${sandbox.rabbitmq.queues.record.created.queue}"},
      containerFactory = "externalValidationFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.created.auto-start:true}")
  public void validateExternal(RecordProcessEvent input) {
    consume(routingKey, input, Step.VALIDATE_EXTERNAL, () -> service.validate(input.getRecord()));
  }
}
