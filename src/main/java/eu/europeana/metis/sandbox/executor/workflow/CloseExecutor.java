package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;
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

  private final AmqpTemplate amqpTemplate;

  @Value("${sandbox.rabbitmq.routing-key.closed}")
  private String routingKey;

  @Value("${sandbox.dataset.max-size}")
  private int maxRecords;

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

    RecordProcessEvent output = new RecordProcessEvent(new RecordInfo(input.getRecord()),
        Step.CLOSE, Status.SUCCESS, maxRecords, new OaiHarvestData("","",""));
    amqpTemplate.convertAndSend(routingKey, output);
  }
}
