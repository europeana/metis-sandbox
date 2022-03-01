package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HarvestOaiPmhExecutor extends StepExecutor {

  private final HarvestService harvestService;
  private final OaiHarvester oaiHarvester;
  private final DatasetService datasetService;

  @Value("${sandbox.rabbitmq.queues.record.created.queue}")
  private String routingKeyCreated;

  @Value("${sandbox.rabbitmq.queues.record.transformation.edm.external.queue}")
  private String routingKeyTransformationToEdmExternal;

  @Autowired
  public HarvestOaiPmhExecutor(AmqpTemplate amqpTemplate, HarvestService service,
                               OaiHarvester oaiHarvester, DatasetService datasetService) {
    super(amqpTemplate);
    this.harvestService = service;
    this.oaiHarvester = oaiHarvester;
    this.datasetService = datasetService;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.harvest.oai.queue}", containerFactory = "harvestOaiPmhFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.harvest.oai.auto-start}")
  public void harvestOaiPmh(RecordProcessEvent input)  {
    String datasetId = input.getRecord().getDatasetId();
    String queueToSend = datasetService.isXsltPresent(datasetId) ? routingKeyTransformationToEdmExternal : routingKeyCreated;
        consume(queueToSend, input, input.getStep(),
            () -> harvestService.harvestOaiRecordHeader(input.getOaiHarvestData(), input.getRecord()));

       
  }

  //This method is only used for testing purposes
  protected void setRoutingKeys(String routingKeyCreated, String routingKeyTransformationToEdmExternal){
    this.routingKeyCreated = routingKeyCreated;
    this.routingKeyTransformationToEdmExternal = routingKeyTransformationToEdmExternal;
  }
}
