package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consumes debias events and performs processing to the contained record <br/> the result will be stored on database
 */
@Component
class DeBiasExecutor extends StepExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasExecutor.class);
  private final DeBiasProcessService service;
  @Value("${sandbox.rabbitmq.queues.record.debias.ready.queue}")
  private String routingKey;

  public DeBiasExecutor(AmqpTemplate amqpTemplate,
      DeBiasProcessService service) {
    super(amqpTemplate);
    this.service = service;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.debias.ready.queue}",
      containerFactory = "deBiasFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformed.auto-start:true}")
  public void debiasProcess(List<RecordProcessEvent> input) {
    input.forEach(r -> LOGGER.info("pulling record {} from queue", r.getRecord().getRecordId()));
    consumeBatch(routingKey, input, Step.DEBIAS,
        () -> service.process(input.stream().map(RecordProcessEvent::getRecord).toList()));
  }
}
