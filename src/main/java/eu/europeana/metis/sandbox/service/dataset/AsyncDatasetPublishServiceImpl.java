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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import eu.europeana.metis.sandbox.service.workflow.HarvestService;
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
  private final String transformationToEdmExternalQueue;
  private final Executor asyncServiceTaskExecutor;
  private final OaiHarvester oaiHarvester;
  private final HarvestService harvestService;
  private final DatasetService datasetService;


  public AsyncDatasetPublishServiceImpl(AmqpTemplate amqpTemplate,
                                        String createdQueue, String transformationToEdmExternalQueue,
                                        Executor asyncServiceTaskExecutor, OaiHarvester oaiHarvester,
                                        HarvestService harvestService, DatasetService datasetService) {
    this.amqpTemplate = amqpTemplate;
    this.createdQueue = createdQueue;
    this.transformationToEdmExternalQueue = transformationToEdmExternalQueue;
    this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
    this.oaiHarvester = oaiHarvester;
    this.harvestService = harvestService;
    this.datasetService = datasetService;
  }


  @Override
  public CompletableFuture<Void> runHarvestOaiAsync(String datasetName, String datasetId,
                                 Country country, Language language, OaiHarvestData oaiHarvestData) {
        return CompletableFuture.runAsync(
                () -> harvestOaiPmh(datasetName, datasetId, country, language, oaiHarvestData), asyncServiceTaskExecutor);

  }

  //If we moved this method to HarvestService while keeping runHarvestOaiAsync method here, it will create a cyclic dependency between
  //this class and HarvestService. Either we keep as it is or we should figure out a solution
  private void harvestOaiPmh(String datasetName, String datasetId,
                            Country country, Language language, OaiHarvestData oaiHarvestData){
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
          datasetService.updateRecordsLimitExceededToTrue(datasetId);
          currentNumberOfIterations.set(maxRecords);
          return ReportingIteration.IterationResult.TERMINATE;
        }

        if(datasetService.isXsltPresent(datasetId)){
          publishToTransformationToEdmExternalQueue(harvestService.harvestOaiRecordHeader(completeOaiHarvestData, recordDataEncapsulated));
        } else {
          publishToCreatedQueue(harvestService.harvestOaiRecordHeader(completeOaiHarvestData, recordDataEncapsulated));

        }

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
            .forEach(recordToPublish -> publishToCreatedQueue(new RecordInfo(recordToPublish))),
        asyncServiceTaskExecutor);
  }

  @Override
  public CompletableFuture<Void> publishWithXslt(Dataset dataset) {
    requireNonNull(dataset, "Dataset must not be null");
    checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

    return CompletableFuture.runAsync(() -> dataset.getRecords()
            .forEach(
                recordToPublish -> publishToTransformationToEdmExternalQueue(new RecordInfo(recordToPublish))),
        asyncServiceTaskExecutor);
  }

  private void publishToCreatedQueue(RecordInfo recordInfo) {
    try {
      amqpTemplate.convertAndSend(createdQueue,
          new RecordProcessEvent(recordInfo, Step.CREATE, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecord().getRecordId(), e);
    }
  }

  private void publishToTransformationToEdmExternalQueue(RecordInfo recordInfo) {
    try {
      amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
          new RecordProcessEvent(recordInfo, Step.CREATE, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecord().getRecordId(), e);
    }
  }

}
