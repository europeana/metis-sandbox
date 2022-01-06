package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes transformed to edm external events and performs external validation to the contained
 * record <br/> Publishes the result in the externally validated queue
 */
@Component
class TransformationToEdmExternalExecutor extends StepExecutor {

  //TODO: Change this class to have Transformation service and to listen to another queue

  private final TransformationService service;

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String routingKey;

  public TransformationToEdmExternalExecutor(AmqpTemplate amqpTemplate,
      TransformationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  //TODO; Change containerFactories names for every executor

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformation.edm.external.queue}",
      containerFactory = "transformationEdmExternalFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformation.edm.external.auto-start:true}")
  public void validateExternal(Event input) {
    consume(routingKey, input, Step.TRANSFORM_TO_EDM_EXTERNAL,
        () -> service.transformToEdmInternal(input.getBody()));
  }

}
