package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.workflow.EnrichmentService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes normalized events and performs enrichment to the contained record <br/> Publishes the
 * result in the enriched queue
 */
@Component
class NormalizedConsumer extends StepConsumer {

  private final EnrichmentService service;

  @Value("${sandbox.rabbitmq.queues.record.enriched.queue}")
  private String routingKey;

  public NormalizedConsumer(AmqpTemplate amqpTemplate,
      EnrichmentService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.normalized.queue}", containerFactory = "normalizedFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.normalized.auto-start:true}")
  public void enrich(Event input) {
    consume(routingKey, input, Step.ENRICH, () -> service.enrich(input.getBody()));
  }
}
