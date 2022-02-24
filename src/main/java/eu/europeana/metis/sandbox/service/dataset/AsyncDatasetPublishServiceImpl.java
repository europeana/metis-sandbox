package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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


    public AsyncDatasetPublishServiceImpl(AmqpTemplate amqpTemplate,
                                          String oaiHarvestedQueue, String createdQueue,
                                          String transformationToEdmExternalQueue, Executor asyncServiceTaskExecutor) {
        this.amqpTemplate = amqpTemplate;
        this.createdQueue = createdQueue;
        this.oaiHarvestedQueue = oaiHarvestedQueue;
        this.transformationToEdmExternalQueue = transformationToEdmExternalQueue;
        this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
    }


    @Override
    public CompletableFuture<Void> harvestOaiPmh(String datasetName, String datasetId, Country country,
                                                 Language language, InputStream xsltInputStream,
                                                 String url, String setspec, String metadataformat) {

        Record record = Record.builder()
                .country(country)
                .language(language)
                .datasetName(datasetName)
                .datasetId(datasetId)
                .content(new byte[0])
                .build();

        List<RecordError> recordErrors = new ArrayList<>();
        RecordInfo recordInfo = new RecordInfo(record, recordErrors);
        RecordProcessEvent event = new RecordProcessEvent(recordInfo, Step.HARVEST_OAI_PMH, Status.SUCCESS,
                maxRecords, url, setspec, metadataformat);

        return CompletableFuture.runAsync(
                () -> this.sendToOaiHarvestQueue(event), asyncServiceTaskExecutor);
    }

    private void sendToOaiHarvestQueue(RecordProcessEvent event) {
        try {
            amqpTemplate.convertAndSend(oaiHarvestedQueue, event);
        } catch (AmqpException e) {
            LOGGER.error("Error sending event to oaiHarvestQueue: ", e);
        }
    }


    @Override
    public CompletableFuture<Void> publishWithoutXslt(Dataset dataset) {
        requireNonNull(dataset, "Dataset must not be null");
        checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

        return CompletableFuture.runAsync(() -> dataset.getRecords()
                .forEach(this::publishToCreatedQueue), asyncServiceTaskExecutor);
    }

    @Override
    public CompletableFuture<Void> publishWithXslt(Dataset dataset) {
        requireNonNull(dataset, "Dataset must not be null");
        checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

        return CompletableFuture.runAsync(() -> dataset.getRecords()
                        .forEach(this::publishToTransformationToEdmExternalQueue),
                asyncServiceTaskExecutor);
    }


    public void publishToCreatedQueue(Record recordData) {
        try {
            amqpTemplate.convertAndSend(createdQueue,
                    new RecordProcessEvent(new RecordInfo(recordData), Step.CREATE, Status.SUCCESS,
                            maxRecords, "", "", ""));
        } catch (AmqpException e) {
            LOGGER.error("There was an issue publishing the record: {} ", recordData.getProviderId(), e);
        }
    }

    private void publishToTransformationToEdmExternalQueue(Record recordData) {
        try {
            amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
                    new RecordProcessEvent(new RecordInfo(recordData), Step.CREATE, Status.SUCCESS,
                            maxRecords, "", "", ""));
        } catch (AmqpException e) {
            LOGGER.error("There was an issue publishing the record: {} ", recordData.getProviderId(), e);
        }
    }

}
