package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.MediaProcessingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnrichedConsumer {

  private AmqpTemplate amqpTemplate;
  private MediaProcessingService service;

  @Value("${sandbox.rabbitmq.queues.record.media.queue}")
  private String routingKey;

  public EnrichedConsumer(AmqpTemplate amqpTemplate,
      MediaProcessingService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }

  //@RabbitListener(queues = "${sandbox.rabbitmq.queues.record.enriched.queue}", containerFactory = "enrichedFactory")
  public void processMedia(Record input) {
    Record output = service.processMedia(input);
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
