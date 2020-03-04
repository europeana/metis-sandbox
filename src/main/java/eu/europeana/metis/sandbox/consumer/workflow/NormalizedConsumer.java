package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.EnrichmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NormalizedConsumer {

  private AmqpTemplate amqpTemplate;
  private EnrichmentService service;

  @Value("${sandbox.rabbitmq.queues.record.enriched.queue}")
  private String routingKey;

  public NormalizedConsumer(AmqpTemplate amqpTemplate,
      EnrichmentService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.normalized.queue}", containerFactory = "normalizedFactory")
  public void enrich(Record input) {
    Record output = service.enrich(input);
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
