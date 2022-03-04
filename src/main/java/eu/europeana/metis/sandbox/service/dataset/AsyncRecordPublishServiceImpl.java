package eu.europeana.metis.sandbox.service.dataset;

import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
class AsyncRecordPublishServiceImpl implements AsyncRecordPublishService {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AsyncRecordPublishServiceImpl.class);

  private final AmqpTemplate amqpTemplate;
  private final String createdQueue;
  private final String transformationToEdmExternalQueue;
  private final Executor asyncServiceTaskExecutor;



  public AsyncRecordPublishServiceImpl(AmqpTemplate amqpTemplate,
                                       String createdQueue, String transformationToEdmExternalQueue,
                                       Executor asyncServiceTaskExecutor) {
    this.amqpTemplate = amqpTemplate;
    this.createdQueue = createdQueue;
    this.transformationToEdmExternalQueue = transformationToEdmExternalQueue;
    this.asyncServiceTaskExecutor = asyncServiceTaskExecutor;
  }


  @Override
  public CompletableFuture<Void> publishWithoutXslt(RecordInfo recordToPublish, Step step) {
    requireNonNull(recordToPublish, "Dataset must not be null");

    return CompletableFuture.runAsync(() -> publishToCreatedQueue(recordToPublish, step),
        asyncServiceTaskExecutor);
  }

  @Override
  public CompletableFuture<Void> publishWithXslt(RecordInfo recordToPublish, Step step) {
    requireNonNull(recordToPublish, "Dataset must not be null");

    return CompletableFuture.runAsync(() -> publishToTransformationToEdmExternalQueue(recordToPublish, step),
        asyncServiceTaskExecutor);
  }

  private void publishToCreatedQueue(RecordInfo recordInfo, Step step) {
    try {
      amqpTemplate.convertAndSend(createdQueue,
          new RecordProcessEvent(recordInfo, step, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecord().getRecordId(), e);
    }
  }

  private void publishToTransformationToEdmExternalQueue(RecordInfo recordInfo, Step step) {
    try {
      amqpTemplate.convertAndSend(transformationToEdmExternalQueue,
          new RecordProcessEvent(recordInfo, step, Status.SUCCESS));
    } catch (AmqpException e) {
      LOGGER.error("There was an issue publishing the record: {} ", recordInfo.getRecord().getRecordId(), e);
    }
  }

}
