package eu.europeana.metis.sandbox.consumer.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

class StepConsumer {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final AmqpTemplate amqpTemplate;

  StepConsumer(AmqpTemplate amqpTemplate) {
    this.amqpTemplate = amqpTemplate;
  }

  public void consume(String routingKey, Event input, Step step,
      Supplier<RecordInfo> recordInfoSupplier) {

    Event output;
    try {
      var recordInfo = recordInfoSupplier.get();
      var status = recordInfo.getErrors().isEmpty() ? Status.SUCCESS : Status.WARN;
      output = new Event(recordInfo, step, status);
    } catch (RecordProcessingException ex) {
      logger.error("Exception while performing step: [{}]. ", step.value(), ex);
      var recordError = new RecordError(ex);
      output = new Event(new RecordInfo(input.getBody(), List.of(recordError)),
          step, Status.FAIL);
    }

    amqpTemplate.convertAndSend(routingKey, output);
  }
}
