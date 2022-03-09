package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes published events to close them
 */
@Component
class CloseExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloseExecutor.class);
  private final AmqpTemplate amqpTemplate;

  @Value("${sandbox.rabbitmq.routing-key.closed}")
  private String routingKey;

  public CloseExecutor(AmqpTemplate amqpTemplate) {
    this.amqpTemplate = amqpTemplate;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.published.queue}",
      containerFactory = "closingFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.published.auto-start:true}")
  public void close(RecordProcessEvent input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }
    try {
      RecordProcessEvent output = new RecordProcessEvent(new RecordInfo(input.getRecord()), Step.CLOSE, Status.SUCCESS);
      amqpTemplate.convertAndSend(routingKey, output);
    } catch (RuntimeException closeException) {
      LOGGER.error("Close executor error", closeException);
    }
  }
}
