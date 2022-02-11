package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.HarvestOaiPmhEvent;
import eu.europeana.metis.sandbox.service.workflow.HarvestServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HarvestOaiPmhExecutor {


  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final AmqpTemplate amqpTemplate;
//  private final HarvestServiceImpl harvestService;

  @Value("${sandbox.rabbitmq.queues.record.harvest-oai.queue}")
  private String harvestOaiQueue;


  public HarvestOaiPmhExecutor(AmqpTemplate amqpTemplate) {
    this.amqpTemplate = amqpTemplate;
//    this.harvestService = harvestService;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.harvest-oai.queue}", containerFactory = "harvestOaiPmhFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.harvest-oai.autostart}")
  public void harvestOaiPmh(HarvestOaiPmhEvent input) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }
    HarvestOaiPmhEvent output;
    try {
      output = new HarvestOaiPmhEvent(input.getStatus(), input.getStep(), input.getUrl(),
          input.getSetspec(), input.getMetadataformat(), input.getOaiRecordId(),
          input.getDatasetId());
    } catch (ServiceException ex) {
      logger.error("Exception in HarvestOaiPmhExecutor", ex);
      input.setStatus(Status.FAIL);
      output = new HarvestOaiPmhEvent(input.getStatus(), input.getStep(), input.getUrl(),
          input.getSetspec(), input.getMetadataformat(), input.getOaiRecordId(),
          input.getDatasetId());
    }
//    logger.info("HarvestOaiPmhExecutor convert and send to queue");
    amqpTemplate.convertAndSend(harvestOaiQueue, output);
//    consume(harvestOaiQueue, output, Step.HARVEST_OAI_PMH, () ->
//        harvestService.harvestOaiPmhToQueue(input);
  }
}
