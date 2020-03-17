package eu.europeana.metis.sandbox.consumer;

import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RecordLogConsumer {

  private RecordLogService recordLogService;

  public RecordLogConsumer(RecordLogService recordLogService) {
    this.recordLogService = recordLogService;
  }

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.log.queue}", containerFactory = "recordLogFactory")
  public void logRecord(Record input) {
    recordLogService.logRecord(input);
  }
}
