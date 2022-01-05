package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes published events to close them
 */
@Component
class CloseConsumer {

  private final AmqpTemplate amqpTemplate;

  @Value("${sandbox.rabbitmq.routing-key.closed}")
  private String routingKey;

  public CloseConsumer(AmqpTemplate amqpTemplate) {
    this.amqpTemplate = amqpTemplate;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.published.queue}", containerFactory = "publishedFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.published.auto-start:true}")
  public void close(Event input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    Event output = new Event(new RecordInfo(input.getBody()), Step.CLOSE, Status.SUCCESS);
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
