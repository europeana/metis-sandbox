package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes debias events and performs processing to the contained record <br/> the result will be stored on database
 */
@Component
class DeBiasExecutor extends StepExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeBiasExecutor.class);
  private final DeBiasProcessService service;

  /**
   * Instantiates a new De bias executor.
   *
   * @param amqpTemplate the amqp template
   * @param service the service
   */
  public DeBiasExecutor(AmqpTemplate amqpTemplate, DeBiasProcessService service) {
    super(amqpTemplate);
    this.service = service;
  }

  /**
   * DeBias process queue.
   *
   * @param input the input
   */
  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.debias.ready.queue}",
      containerFactory = "deBiasFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.transformed.auto-start:true}")
  public void debiasProcess(List<RecordProcessEvent> input) {
    input.forEach(r -> LOGGER.debug("pulling record {} from queue", r.getRecord().getRecordId()));
    service.process(input.stream().map(RecordProcessEvent::getRecord).toList());
  }
}
