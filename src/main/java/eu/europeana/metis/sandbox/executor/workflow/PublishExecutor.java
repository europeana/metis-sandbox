package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.IndexEnvironment;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes previewed events and publish
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

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.previewed.queue}",
      containerFactory = "publishFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.previewed.auto-start:true}")
  public void publish(Event input) {
    consume(routingKey, input, Step.PUBLISH,
        () -> service.index(input.getBody(), IndexEnvironment.PUBLISH));
  }
}
