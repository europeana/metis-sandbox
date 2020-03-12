package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.slf4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExternallyValidatedConsumer {

  private AmqpTemplate amqpTemplate;
  private TransformationService service;

  @Value("${sandbox.rabbitmq.queues.record.transformed.queue}")
  private String routingKey;

  public ExternallyValidatedConsumer(AmqpTemplate amqpTemplate,
      TransformationService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  // TODO keep consuming from here
  //@RabbitListener(queues = "${sandbox.rabbitmq.queues.record.validated.external.queue}", containerFactory = "externallyValidatedFactory")
  public void transform(Record input) {
    Record output = service.transform(input);
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
