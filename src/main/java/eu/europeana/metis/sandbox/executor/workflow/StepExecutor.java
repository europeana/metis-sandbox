package eu.europeana.metis.sandbox.executor.workflow;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.domain.RecordError;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;

/**
 * Parent class for all consumer steps, generalizes the consumers action
 */
class StepExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(StepExecutor.class);
  private final AmqpTemplate amqpTemplate;

  /**
   * Instantiates a new Step executor.
   *
   * @param amqpTemplate the amqp template
   */
  StepExecutor(AmqpTemplate amqpTemplate) {
    this.amqpTemplate = amqpTemplate;
  }

  /**
   * Consume.
   *
   * @param routingKey the routing key
   * @param input the input
   * @param step the step
   * @param recordInfoSupplier the record info supplier
   */
  public void consume(String routingKey, RecordProcessEvent input, Step step,
      Supplier<RecordInfo> recordInfoSupplier) {
    if (input.getStatus() == Status.FAIL) {
      return;
    }

    RecordProcessEvent output;
    try {
      var recordInfo = recordInfoSupplier.get();
      var status = recordInfo.getErrors().isEmpty() ? Status.SUCCESS : Status.WARN;
      output = new RecordProcessEvent(recordInfo, step, status);
    } catch (RecordProcessingException ex) {
      output = createFailEvent(input, step, ex);
    } catch (Exception ex) {
      //Also catch runtime exceptions to avoid losing the message or thread
      output = createFailEvent(input, step, new RecordProcessingException(Long.toString(input.getRecord().getRecordId()), ex));
    }
    try {
      amqpTemplate.convertAndSend(routingKey, output);
    } catch (RuntimeException rabbitException) {
      LOGGER.error("Queue step execution error", rabbitException);
    }
  }

  /**
   * Consume batch.
   *
   * @param routingKey the routing key
   * @param input the input
   * @param step the step
   * @param recordInfoSupplier the record info supplier
   */
  public void consumeBatch(String routingKey, List<RecordProcessEvent> input, Step step,
      Supplier<List<RecordInfo>> recordInfoSupplier) {
    List<RecordProcessEvent> outputRecordProcessEvents = new ArrayList<>();
    try {
      var listRecordInfo = recordInfoSupplier.get();
      for (RecordInfo recordInfo : listRecordInfo) {
        var status = recordInfo.getErrors().isEmpty() ? Status.SUCCESS : Status.WARN;
        outputRecordProcessEvents.add(new RecordProcessEvent(recordInfo, step, status));
      }
    } catch (RecordProcessingException ex) {
      for (RecordProcessEvent recordProcessEvent : input) {
        outputRecordProcessEvents.add(createFailEvent(recordProcessEvent, step, ex));
      }
    } catch (Exception ex) {
      for (RecordProcessEvent recordProcessEvent : input) {
        outputRecordProcessEvents.add(
            createFailEvent(recordProcessEvent, step,
                new RecordProcessingException(Long.toString(recordProcessEvent.getRecord().getRecordId()), ex)
            )
        );
      }
    }
    try {
      outputRecordProcessEvents.forEach(output -> amqpTemplate.convertAndSend(routingKey, output));
    } catch (RuntimeException rabbitException) {
      LOGGER.error("Queue step execution error", rabbitException);
    }
  }

  private RecordProcessEvent createFailEvent(RecordProcessEvent input, Step step, RecordProcessingException ex) {
    final String stepName = step.value();
    final RecordError recordError = new RecordError(ex);
    final RecordProcessEvent output = new RecordProcessEvent(new RecordInfo(input.getRecord(), List.of(recordError)), step,
        Status.FAIL);
    LOGGER.error("Exception while performing step: [{}]. ", stepName, ex);
    return output;
  }
}
