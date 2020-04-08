package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.EnrichmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NormalizedConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(NormalizedConsumer.class);

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
  public void enrich(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output;
    Record record;
    try {
      record = service.enrich(input.getBody());
      output = new Event(record, Step.ENRICH);
    } catch (RecordProcessingException ex) {
      LOGGER.error("Error validating record", ex);
      record = Record.from(input.getBody(), input.getBody().getContent());
      output = new Event(record, Step.ENRICH, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
