package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.NormalizationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes internally validated events and performs normalization to the contained record <br/>
 * Publishes the result in the normalized queue
 */
@Component
class NormalizationExecutor extends StepExecutor {

  private final NormalizationService service;

  @Value("${sandbox.rabbitmq.queues.record.normalized.queue}")
  private String routingKey;

  public NormalizationExecutor(AmqpTemplate amqpTemplate,
      NormalizationService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.validated.internal.queue}",
      containerFactory = "normalizationFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.validated.internal.auto-start:true}")
  public void normalize(Event input) {
    consume(routingKey, input, Step.NORMALIZE, () -> service.normalize(input.getBody()));
  }
}
