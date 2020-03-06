package eu.europeana.metis.sandbox.consumer;

import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventRecordLogConsumer {

  private RecordLogService recordLogService;

  public EventRecordLogConsumer(RecordLogService recordLogService) {
    this.recordLogService = recordLogService;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.log.queue}", containerFactory = "recordLogFactory")
  public void logRecord(Event<Record> input) {
    recordLogService.logRecordEvent(input);
  }
}
