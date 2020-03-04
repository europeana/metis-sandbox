package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransformedConsumer {

  private AmqpTemplate amqpTemplate;
  private InternalValidationService service;

  @Value("${sandbox.rabbitmq.queues.record.validated.internal.queue}")
  private String routingKey;

  public TransformedConsumer(AmqpTemplate amqpTemplate,
      InternalValidationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.transformed.queue}", containerFactory = "transformedFactory")
  public void validateInternal(Record input) {
    service.validate(input);
    amqpTemplate.convertAndSend(routingKey, input);
  }
}
