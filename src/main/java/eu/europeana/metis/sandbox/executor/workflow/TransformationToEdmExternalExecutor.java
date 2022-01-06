package eu.europeana.metis.sandbox.executor.workflow;

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
class TransformationToEdmExternalExecutor extends StepExecutor {

  //TODO: Change this class to have Transformation service and to listen to another queue

  private final ExternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String routingKey;

  public TransformationToEdmExternalExecutor(AmqpTemplate amqpTemplate,
      ExternalValidationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  //TODO; Change containerFactories names for every executor

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformation.edm.external.queue}", containerFactory = "transformationEdmExternalFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformation.edm.external.auto-start:true}")
  public void validateExternal(Event input) {
    consume(routingKey, input, Step.VALIDATE_EXTERNAL, () -> service.validate(input.getBody()));
  }

}
