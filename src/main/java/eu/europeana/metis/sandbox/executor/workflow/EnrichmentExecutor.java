package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
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
class EnrichmentExecutor extends StepExecutor {

  private final EnrichmentService service;

  @Value("${sandbox.rabbitmq.queues.record.enriched.queue}")
  private String routingKey;

  public EnrichmentExecutor(AmqpTemplate amqpTemplate,
      EnrichmentService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.normalized.queue}",
      containerFactory = "enrichmentFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.normalized.auto-start:true}")
  public void enrich(RecordProcessEvent event) {
    consume(routingKey, event, Step.ENRICH, () -> service.enrich(event.getRecord()));
  }
}
