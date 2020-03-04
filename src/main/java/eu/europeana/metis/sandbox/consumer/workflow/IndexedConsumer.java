package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IndexedConsumer {

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.indexed.queue}", containerFactory = "indexedFactory")
  public void close(Record input) {
    log.info("Record {} is closed", input.getDatasetId());
  }
}
