package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.ExternalValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class CreatedConsumer {

  private AmqpTemplate amqpTemplate;
  private ExternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.external.queue}")
  private String routingKey;

  public CreatedConsumer(AmqpTemplate amqpTemplate,
      ExternalValidationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.created.queue}", containerFactory = "createdFactory")
  public void applyOrdering(Record input) {
    Record output = service.validate(input);
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
