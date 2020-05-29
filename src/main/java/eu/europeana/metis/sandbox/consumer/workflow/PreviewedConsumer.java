package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.IndexEnvironment;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes events in preview and publish them
 */
@Component
class PreviewedConsumer extends StepConsumer {

  private final IndexingService service;

  @Value("${sandbox.rabbitmq.queues.record.published.queue}")
  private String routingKey;

  public PreviewedConsumer(AmqpTemplate amqpTemplate,
      IndexingService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.previewed.queue}", containerFactory = "previewedFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.previewed.auto-start:true}")
  public void publish(Event input) {
    consume(routingKey, input, Step.PUBLISH,
        () -> service.index(input.getBody(), IndexEnvironment.PUBLISH));
  }
}
