package eu.europeana.metis.sandbox.consumer.workflow;

import static java.lang.String.format;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

class AmqpConsumer {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  protected AmqpTemplate amqpTemplate;

  protected void processEvent(Event event, Step step, Supplier<Record> supplier,
      String routingKey) {
    Event output;
    Record record;
    try {
      record = supplier.get();
      output = new Event(record, step);
    } catch (RecordProcessingException ex) {
      logger.error(format("Error processing record %s on step %s ", ex.getRecordId(), step), ex);
      record = Record.from(event.getBody(), event.getBody().getContent());
      output = new Event(record, step, new EventError(ex));
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
