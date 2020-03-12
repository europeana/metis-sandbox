package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.domain.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class IndexedConsumer {

  private static final Logger log = LoggerFactory.getLogger(IndexedConsumer.class);

  @RabbitListener(queues = "${sandbox.rabbitmq.queues.record.indexed.queue}", containerFactory = "indexedFactory")
  public void close(Record input) {
    log.info("Record {} is closed", input.getDatasetId());
  }
}
