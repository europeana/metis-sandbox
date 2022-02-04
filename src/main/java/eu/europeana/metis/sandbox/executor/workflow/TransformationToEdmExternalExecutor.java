package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
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

  private final TransformationService service;

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String routingKey;

  public TransformationToEdmExternalExecutor(AmqpTemplate amqpTemplate,
      TransformationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformation.edm.external.queue}",
      containerFactory = "transformationEdmExternalFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformation.edm.external.auto-start:true}")
  public void transformationToEdmExternal(RecordProcessEvent event) {
    consume(routingKey, event, Step.TRANSFORM_TO_EDM_EXTERNAL,
        () -> service.transform(event.getRecord()));
  }

}
