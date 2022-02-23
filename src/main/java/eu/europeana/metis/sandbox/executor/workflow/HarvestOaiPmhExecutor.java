package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration.IterationResult;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HarvestOaiPmhExecutor extends StepExecutor {

  private final HarvestService harvestService;
  private final OaiHarvester oaiHarvester;
  //TODO Add DatasetRepository

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String routingKey;


  public HarvestOaiPmhExecutor(AmqpTemplate amqpTemplate, HarvestService service,
                               OaiHarvester oaiHarvester) {
    super(amqpTemplate);
    this.harvestService = service;
    this.oaiHarvester = oaiHarvester;

  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.harvest.oai.queue}", containerFactory = "harvestOaiPmhFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.harvest.oai.auto-start}")
  public void harvestOaiPmh(RecordProcessEvent input)  {
    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester
        .harvestRecordHeaders(
            new OaiHarvest(input.getUrl(), input.getMetadataformat(), input.getSetspec()))) {

      AtomicInteger currentNumberOfIterations = new AtomicInteger();

      recordHeaderIterator.forEach(recordHeader -> {
        currentNumberOfIterations.getAndIncrement();

        if (currentNumberOfIterations.get() > input.getMaxRecords()) {
          return IterationResult.TERMINATE;
          //TODO: Update dataset recordLimitExceeded
        }
        // send to next queue, in this case: sandbox.rabbitmq.queues.record.created.queue
        consume(routingKey, input, input.getStep(),
            () -> harvestService.harvestOaiRecordHeader(input, recordHeader, input.getDatasetId()));
          //TODO: Update dataset recordsQuantity
        return IterationResult.CONTINUE;
      });

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }
  }
}
