package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.DeBiasProcessService;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
      autoStartup = "${sandbox.rabbitmq.queues.record.debias.ready.auto-start:true}")
  public void debiasProcess(List<RecordProcessEvent> input) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("pulling records from queue: {}", input.stream()
                                                          .map(item -> item.getRecord()
                                                                           .getRecordId()
                                                                           .toString())
                                                          .collect(Collectors.joining(",")));
    }
  }
}
