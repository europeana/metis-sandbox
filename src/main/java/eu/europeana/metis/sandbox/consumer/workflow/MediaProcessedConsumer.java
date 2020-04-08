package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MediaProcessedConsumer {

  private AmqpTemplate amqpTemplate;
  private IndexingService service;

  @Value("${sandbox.rabbitmq.queues.record.indexed.queue}")
  private String routingKey;

  public MediaProcessedConsumer(AmqpTemplate amqpTemplate,
      IndexingService service) {
    this.amqpTemplate = amqpTemplate;
    this.service = service;
  }


  // TODO keep consuming from here
  // @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.media.queue}", containerFactory = "mediaProcessedFactory")
  public void index(Event input) {
    service.index(input.getBody());
    amqpTemplate.convertAndSend(routingKey, input);
  }
}
