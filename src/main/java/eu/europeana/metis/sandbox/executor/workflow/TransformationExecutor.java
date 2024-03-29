package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes externally validated events and performs transformation to the contained record <br/>
 * Publishes the result in the transformed queue
 */
@Component
class TransformationExecutor extends StepExecutor {

  private final TransformationService service;

  @Value("${sandbox.rabbitmq.queues.record.transformed.queue}")
  private String routingKey;

  public TransformationExecutor(AmqpTemplate amqpTemplate,
      TransformationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.validated.external.queue}",
      containerFactory = "transformationFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.validated.external.auto-start:true}")
  public void transform(RecordProcessEvent input) {
    consume(routingKey, input, Step.TRANSFORM, () -> service.transformToEdmInternal(input.getRecord()));
  }
}
