package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.NormalizationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternallyValidatedConsumer {

  private AmqpTemplate amqpTemplate;
  private NormalizationService service;

  @Value("${sandbox.rabbitmq.queues.record.normalized.queue}")
  private String routingKey;

  public InternallyValidatedConsumer(AmqpTemplate amqpTemplate,
      NormalizationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.validated.internal.queue}", containerFactory = "internallyValidatedFactory")
  public void normalize(Record input) {
    Record output = service.normalize(input);
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
