package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

/**
 * Unit test for {@link StepExecutor}
 *
 * @author Jorge Ortiz
 * @since 01-03-2022
 */
@ExtendWith(MockitoExtension.class)
class StepExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;

  @InjectMocks
  private StepExecutor stepExecutor;

  @Test
  void consumeEventStatusSuccess_expectEventSuccess() {
    final String routingKey = "routingKey";
    final Record myTestRecord = getTestRecord(1L);
    final RecordProcessEvent myEvent = new RecordProcessEvent(new RecordInfo(myTestRecord), Step.HARVEST_FILE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.HARVEST_FILE, this::getRecordInfo);

    verify(amqpTemplate).convertAndSend(eq(routingKey), captor.capture());

    assertRecordId_CreatedStepAndStatus(getRecordInfo().getRecordValue().getRecordId(), Status.SUCCESS);
  }

  @Test
  void consumeEventStatusFailQueueNeverCalled_expectException() {
    final String routingKey = "routingKey";
    final Record myTestRecord = getTestRecord(1L);
    final RecordProcessEvent myEvent = new RecordProcessEvent(new RecordInfo(myTestRecord), Step.HARVEST_FILE, Status.FAIL);

    stepExecutor.consume(routingKey, myEvent, Step.HARVEST_FILE, this::getRecordInfo);

    verify(amqpTemplate, never()).convertAndSend(eq(routingKey), captor.capture());

    assertThrows(MockitoException.class, () -> captor.getValue());
  }

  @Test
  void consumeEventStatusSuccessThrowsRuntimeException_expectEventFail() {
    final String routingKey = "routingKey";
    final long expectedRecordId = 1L;
    final Record myTestRecord = getTestRecord(expectedRecordId);
    final RecordProcessEvent myEvent = new RecordProcessEvent(new RecordInfo(myTestRecord), Step.HARVEST_FILE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.HARVEST_FILE, () ->
    {
      throw new RuntimeException("General Failure");
    });

    verify(amqpTemplate).convertAndSend(eq(routingKey), captor.capture());

    assertRecordId_CreatedStepAndStatus(expectedRecordId, Status.FAIL);
  }

  @Test
  void consumeEventStatusSuccessThrowsRecordProcessingException_expectEventFail() {
    final String routingKey = "routingKey";
    final long expectedRecordId = 2L;
    final Record myTestRecord = getTestRecord(expectedRecordId);
    final RecordProcessEvent myEvent = new RecordProcessEvent(new RecordInfo(myTestRecord), Step.HARVEST_FILE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.HARVEST_FILE, () ->
    {
      throw new RecordProcessingException("2", new Throwable("Record failure"));
    });

    verify(amqpTemplate).convertAndSend(eq(routingKey), captor.capture());

    assertRecordId_CreatedStepAndStatus(expectedRecordId, Status.FAIL);
  }

  @Test
  void consumeEventStatusSuccessThrowsRabbitMQException_expectEventSuccess() {
    final String routingKey = "routingKey";
    final long expectedRecordId = 2L;
    final Record myTestRecord = getTestRecord(expectedRecordId);
    final RecordProcessEvent myEvent = new RecordProcessEvent(new RecordInfo(myTestRecord), Step.HARVEST_FILE, Status.SUCCESS);
    final RuntimeException runtimeException = new AmqpException("Queue Failure");
    doThrow(runtimeException)
        .when(amqpTemplate)
        .convertAndSend(eq(routingKey), any(Object.class));

    assertDoesNotThrow(() -> stepExecutor.consume(routingKey, myEvent, Step.HARVEST_FILE, this::getRecordInfo));
  }

  private void assertRecordId_CreatedStepAndStatus(final Long RecordId, final Status status) {
    assertEquals(RecordId, captor.getValue().getRecord().getRecordId());
    assertEquals(Step.HARVEST_FILE, captor.getValue().getStep());
    assertEquals(status, captor.getValue().getStatus());
  }

  @NotNull
  private RecordInfo getRecordInfo() {
    final Record mySecondRecord = getTestRecord(2L);
    return new RecordInfo(mySecondRecord);
  }

  private Record getTestRecord(final long recordId) {
    return Record.builder().recordId(recordId)
                 .datasetId("1")
                 .datasetName("")
                 .country(Country.FINLAND)
                 .language(Language.FI)
                 .content("".getBytes())
                 .build();
  }
}
