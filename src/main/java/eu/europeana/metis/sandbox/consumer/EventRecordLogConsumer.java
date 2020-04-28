package eu.europeana.metis.sandbox.consumer;

import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.service.record.RecordStoreService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes all events
 * <br/>
 * Save events to persistence store
 */
@Component
class EventRecordLogConsumer {

  private final RecordStoreService recordStoreService;

  public EventRecordLogConsumer(RecordStoreService recordStoreService) {
    this.recordStoreService = recordStoreService;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.log.queue}", containerFactory = "recordLogFactory")
  public void logRecord(Event input) {
    recordStoreService.storeRecordEvent(input);
  }
}
