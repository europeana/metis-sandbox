package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes media processing events and publish
 * <br />
 * Publishes the result in the published queue
 */
@Component
class PublishExecutor extends StepExecutor {

  private final IndexingService service;

  @Value("${sandbox.rabbitmq.queues.record.published.queue}")
  private String routingKey;

  public PublishExecutor(AmqpTemplate amqpTemplate,
      IndexingService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.media.queue}",
      containerFactory = "publishFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.media.auto-start:true}")
  public void publish(RecordProcessEvent input) {
    consume(routingKey, input, Step.PUBLISH,
        () -> service.index(input.getRecord()));
  }
}
