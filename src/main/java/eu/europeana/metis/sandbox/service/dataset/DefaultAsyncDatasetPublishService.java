package eu.europeana.metis.sandbox.service.dataset;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class DefaultAsyncDatasetPublishService implements AsyncDatasetPublishService {

  private AmqpTemplate amqpTemplate;
  private String initialQueue;
  private Executor taskExecutor;

  public DefaultAsyncDatasetPublishService(AmqpTemplate amqpTemplate,
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
    try {
      amqpTemplate.convertAndSend(initialQueue, record);
    } catch (Exception e) {
      log.error("There was an issue publishing the record: {} {}", record.getRecordId(), e.getMessage(), e);
      throw new ServiceException("There was an issue publishing the record: " + e.getMessage(), e);
    }
  }
}
