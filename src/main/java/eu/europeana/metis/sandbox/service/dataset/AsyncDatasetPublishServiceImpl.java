package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
class AsyncDatasetPublishServiceImpl implements AsyncDatasetPublishService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(AsyncDatasetPublishServiceImpl.class);


  private final AmqpTemplate amqpTemplate;
  private final String createdQueue;
  private final String transformationToEdmExternalQueue;
  private final Executor asyncDatasetPublishServiceTaskExecutor;

  public AsyncDatasetPublishServiceImpl(AmqpTemplate amqpTemplate,
      String createdQueue, String transformationToEdmExternalQueue,
      Executor asyncDatasetPublishServiceTaskExecutor) {
    this.amqpTemplate = amqpTemplate;
    this.createdQueue = createdQueue;
    this.asyncDatasetPublishServiceTaskExecutor = asyncDatasetPublishServiceTaskExecutor;
    this.transformationToEdmExternalQueue = transformationToEdmExternalQueue;
  }

  @Override
  public CompletableFuture<Void> publish(Dataset dataset, boolean hasXsltToEdmExternal) {
    requireNonNull(dataset, "Dataset must not be null");
    checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

    return CompletableFuture.runAsync(() -> dataset.getRecords()
        .forEach(record -> this.publish(record, hasXsltToEdmExternal)), asyncDatasetPublishServiceTaskExecutor);
  }

  private void publish(Record record, boolean hasXsltToEdmExternal) {
    try {
      if(hasXsltToEdmExternal){
        amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
            new RecordProcessEvent(new RecordInfo(record), Step.CREATE, Status.SUCCESS));
      } else {
        amqpTemplate.convertAndSend(createdQueue,
            new RecordProcessEvent(new RecordInfo(record), Step.CREATE, Status.SUCCESS));
      }
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", record.getProviderId(), e);
    }
  }
}
