package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
class AsyncDatasetPublishServiceImpl implements AsyncDatasetPublishService {

  private static final Logger log = LoggerFactory
      .getLogger(AsyncDatasetPublishServiceImpl.class);

  private AmqpTemplate amqpTemplate;
  private String initialQueue;
  private Executor taskExecutor;

  public AsyncDatasetPublishServiceImpl(AmqpTemplate amqpTemplate,
      String initialQueue,
      Executor taskExecutor) {
    this.amqpTemplate = amqpTemplate;
    this.initialQueue = initialQueue;
    this.taskExecutor = taskExecutor;
  }

  @Override
  public CompletableFuture<Void> publish(Dataset dataset) {
    requireNonNull(dataset, "Dataset must not be null");
    checkArgument(!dataset.getRecords().isEmpty(), "Dataset records must no be empty");

    return CompletableFuture.runAsync(() -> dataset.getRecords()
        .forEach(this::publish), taskExecutor);
  }

  private void publish(Record record) {
    Event<Record> recordEvent = new Event<>(record, Step.CREATE);
    try {
      amqpTemplate.convertAndSend(initialQueue, recordEvent);
    } catch (Exception e) {
      log.error("There was an issue publishing the record: {} {}", record.getRecordId(), e.getMessage(), e);
      throw new RecordProcessingException(record.getRecordId(), e);
    }
  }
}
