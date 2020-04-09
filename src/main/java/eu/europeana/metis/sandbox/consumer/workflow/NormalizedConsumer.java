package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.EnrichmentService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes normalized events and performs enrichment to the contained record <br/> Publishes the
 * result in the enriched queue
 */
@Component
class NormalizedConsumer {

  private AmqpTemplate amqpTemplate;
  private EnrichmentService service;

  @Value("${sandbox.rabbitmq.queues.record.enriched.queue}")
  private String routingKey;

  public NormalizedConsumer(AmqpTemplate amqpTemplate,
      EnrichmentService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  // TODO continue here
  //@RabbitListener(queues = "${sandbox.rabbitmq.queues.record.normalized.queue}", containerFactory = "normalizedFactory")
  public void enrich(Event input) {
    Record output = service.enrich(input.getBody());
    amqpTemplate.convertAndSend(routingKey, input);
  }
}
