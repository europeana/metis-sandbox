package eu.europeana.metis.sandbox.executor;

import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumes all events
 * <br/>
 * Save events to persistence store
 */
@Component
class EventRecordLogConsumer {

  private final RecordLogService recordLogService;

  public EventRecordLogConsumer(RecordLogService recordLogService) {
    this.recordLogService = recordLogService;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.log.queue}", containerFactory = "recordLogFactory",
      autoStartup = "${sandbox.rabbitmq.queues.record.log.auto-start:true}")
  public void logRecord(RecordProcessEvent input) {
    recordLogService.logRecordEvent(input);
  }
}
