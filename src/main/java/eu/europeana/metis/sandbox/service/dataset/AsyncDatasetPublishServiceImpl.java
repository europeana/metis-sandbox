package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;

import java.io.IOException;;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class AsyncDatasetPublishServiceImpl implements AsyncDatasetPublishService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AsyncDatasetPublishServiceImpl.class);

  @Value("${sandbox.dataset.max-size}")
  private int maxRecords;
  private final AmqpTemplate amqpTemplate;
  private final String createdQueue;
  private final String oaiHarvestedQueue;
  private final String transformationToEdmExternalQueue;
  private final Executor asyncServiceTaskExecutor;
  private final OaiHarvester oaiHarvester;
  private final DatasetService datasetService;


  public AsyncDatasetPublishServiceImpl(AmqpTemplate amqpTemplate,
                                        String oaiHarvestedQueue, String createdQueue, String transformationToEdmExternalQueue,
                                        Executor asyncServiceTaskExecutor, OaiHarvester oaiHarvester, DatasetService datasetService) {
    this.amqpTemplate = amqpTemplate;
    this.createdQueue = createdQueue;
    this.oaiHarvestedQueue = oaiHarvestedQueue;
    this.transformationToEdmExternalQueue = transformationToEdmExternalQueue;
    this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
    this.oaiHarvester = oaiHarvester;
    this.datasetService = datasetService;
  }


  @Override
  public void harvestOaiPmh(String datasetName, String datasetId,
      Country country, Language language, OaiHarvestData oaiHarvestData) {

    try (OaiRecordHeaderIterator recordHeaderIterator = oaiHarvester
            .harvestRecordHeaders(
                    new OaiHarvest(oaiHarvestData.getUrl(), oaiHarvestData.getMetadataformat(), oaiHarvestData.getSetspec()))) {

      AtomicInteger currentNumberOfIterations = new AtomicInteger();

      Record recordDataEncapsulated = Record.builder()
              .country(country)
              .language(language)
              .datasetName(datasetName)
              .datasetId(datasetId)
              .content(new byte[0])
              .build();

      recordHeaderIterator.forEach(recordHeader -> {
        OaiHarvestData completeOaiHarvestData = new OaiHarvestData(oaiHarvestData.getUrl(),
                oaiHarvestData.getSetspec(),
                oaiHarvestData.getMetadataformat(),
                recordHeader.getOaiIdentifier());
        currentNumberOfIterations.getAndIncrement();

        if (currentNumberOfIterations.get() > maxRecords) {
          datasetService.updateRecordsLimitExceeded(datasetId);
          currentNumberOfIterations.set(maxRecords);
          return ReportingIteration.IterationResult.TERMINATE;
        }
        // send to next queue, in this case: sandbox.rabbitmq.queues.record.created.queue
        CompletableFuture.runAsync(
                () -> this.sendToOaiHarvestQueue(recordDataEncapsulated, completeOaiHarvestData), asyncServiceTaskExecutor);

        return ReportingIteration.IterationResult.CONTINUE;
      });

      datasetService.updateNumberOfTotalRecord(datasetId, currentNumberOfIterations.get());

    } catch (HarvesterException | IOException e) {
      throw new ServiceException("Error harvesting OAI-PMH records ", e);
    }

  }


  @Override
  public CompletableFuture<Void> publishWithoutXslt(Dataset dataset) {
    requireNonNull(dataset, "Dataset must not be null");
    checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

    return CompletableFuture.runAsync(() -> dataset.getRecords()
            .forEach(this::publishToCreatedQueue),
        asyncServiceTaskExecutor);
  }

  @Override
  public CompletableFuture<Void> publishWithXslt(Dataset dataset) {
    requireNonNull(dataset, "Dataset must not be null");
    checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

    return CompletableFuture.runAsync(() -> dataset.getRecords()
            .forEach(
                this::publishToTransformationToEdmExternalQueue),
        asyncServiceTaskExecutor);
  }


  private void publishToCreatedQueue(Record recordData) {
    try {
      amqpTemplate.convertAndSend(createdQueue,
          new RecordProcessEvent(new RecordInfo(recordData), Step.CREATE, Status.SUCCESS,
                  new OaiHarvestData()));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordData.getProviderId(), e);
    }
  }

  private void publishToTransformationToEdmExternalQueue(Record recordData) {
    try {
      amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
          new RecordProcessEvent(new RecordInfo(recordData), Step.CREATE, Status.SUCCESS,
              new OaiHarvestData()));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordData.getProviderId(), e);
    }
  }

  private void sendToOaiHarvestQueue(Record recordData, OaiHarvestData oaiHarvestData) {
    try {
      amqpTemplate.convertAndSend(oaiHarvestedQueue, new RecordProcessEvent(new RecordInfo(recordData), Step.HARVEST_OAI_PMH,
              Status.SUCCESS, oaiHarvestData));
    } catch (AmqpException e) {
      LOGGER.error("Error sending event to oaiHarvestQueue: ", e);
    }
  }

}
