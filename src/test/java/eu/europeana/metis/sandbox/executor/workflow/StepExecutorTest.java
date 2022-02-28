package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import nl.altindag.log.LogCaptor;
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
 * @author Jorge Ortiz
 */
@ExtendWith(MockitoExtension.class)
class StepExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Captor
  private ArgumentCaptor<Event> captor;

  @InjectMocks
  private StepExecutor stepExecutor;

  @Test
  void consumeEventStatusSuccess_expectEventSuccess() {
    final String routingKey = "routingKey";
    final Record myTestRecord = Record.builder().recordId(1L)
                                      .datasetId("1")
                                      .datasetName("")
                                      .country(Country.FINLAND)
                                      .language(Language.FI)
                                      .content("".getBytes())
                                      .build();
    final Event myEvent = new Event(new RecordInfo(myTestRecord), Step.CREATE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.CREATE, () -> getRecordInfo());

    verify(amqpTemplate).convertAndSend(eq(routingKey), captor.capture());

    assertEquals(Step.CREATE, captor.getValue().getStep());
    assertEquals(getRecordInfo().getRecord().getRecordId(), captor.getValue().getBody().getRecordId());
    assertEquals(Status.SUCCESS, captor.getValue().getStatus());
  }

  @Test
  void consumeEventStatusFailQueueNeverCalled_expectException() {
    final String routingKey = "routingKey";
    final Record myTestRecord = Record.builder().recordId(1L)
                                      .datasetId("1")
                                      .datasetName("")
                                      .country(Country.FINLAND)
                                      .language(Language.FI)
                                      .content("".getBytes())
                                      .build();
    final Event myEvent = new Event(new RecordInfo(myTestRecord), Step.CREATE, Status.FAIL);

    stepExecutor.consume(routingKey, myEvent, Step.CREATE, () -> getRecordInfo());

    verify(amqpTemplate, never()).convertAndSend(eq(routingKey), captor.capture());

    assertThrows(MockitoException.class, () -> captor.getValue());
  }

  @Test
  void consumeEventStatusSuccessThrowsRuntimeException_expectEventFail() {
    final String routingKey = "routingKey";
    final Long expectedRecordId = 1L;
    final Record myTestRecord = Record.builder().recordId(expectedRecordId)
                                      .datasetId("1")
                                      .datasetName("")
                                      .country(Country.FINLAND)
                                      .language(Language.FI)
                                      .content("".getBytes())
                                      .build();
    final Event myEvent = new Event(new RecordInfo(myTestRecord), Step.CREATE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.CREATE, () ->
    {
      throw new RuntimeException("General Failure");
    });

    verify(amqpTemplate).convertAndSend(eq(routingKey), captor.capture());

    assertEquals(Step.CREATE, captor.getValue().getStep());
    assertEquals(expectedRecordId, captor.getValue().getBody().getRecordId());
    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }

  @Test
  void consumeEventStatusSuccessThrowsRecordProcessingException_expectEventFail() {
    final String routingKey = "routingKey";
    final Long expectedRecordId = 2L;
    final Record myTestRecord = Record.builder().recordId(expectedRecordId)
                                      .datasetId("1")
                                      .datasetName("")
                                      .country(Country.FINLAND)
                                      .language(Language.FI)
                                      .content("".getBytes())
                                      .build();
    final Event myEvent = new Event(new RecordInfo(myTestRecord), Step.CREATE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.CREATE, () ->
    {
      throw new RecordProcessingException("2", new Throwable("Record failure"));
    });

    verify(amqpTemplate).convertAndSend(eq(routingKey), captor.capture());

    assertEquals(Step.CREATE, captor.getValue().getStep());
    assertEquals(expectedRecordId, captor.getValue().getBody().getRecordId());
    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }

  @Test
  void consumeEventStatusSuccessThrowsRabbitMQException_expectEventSuccess() {
    final LogCaptor logCaptor = LogCaptor.forClass(StepExecutor.class);
    final String routingKey = "routingKey";
    final Long expectedRecordId = 2L;
    final Record myTestRecord = Record.builder().recordId(expectedRecordId)
                                      .datasetId("1")
                                      .datasetName("")
                                      .country(Country.FINLAND)
                                      .language(Language.FI)
                                      .content("".getBytes())
                                      .build();
    final Event myEvent = new Event(new RecordInfo(myTestRecord), Step.CREATE, Status.SUCCESS);
    final RuntimeException runtimeException = new AmqpException("Queue Failure");
    doThrow(runtimeException)
        .when(amqpTemplate)
        .convertAndSend(eq(routingKey), any(Object.class));

    stepExecutor.consume(routingKey, myEvent, Step.CREATE, () -> getRecordInfo());

    assertLogCaptor(logCaptor);
  }

  private void assertLogCaptor(LogCaptor logCaptor) {
    assertEquals(1, logCaptor.getErrorLogs().size());
    final String testMessage = logCaptor.getErrorLogs().stream().findFirst().get();
    assertTrue( testMessage.contains("Queue step execution error"));
  }

  @NotNull
  private RecordInfo getRecordInfo() {
    final Record mySecondRecord = Record.builder().recordId(2L)
                                        .datasetId("1")
                                        .datasetName("")
                                        .country(Country.FINLAND)
                                        .language(Language.FI)
                                        .content("".getBytes())
                                        .build();
    final RecordInfo recordInfo = new RecordInfo(mySecondRecord);
    return recordInfo;
  }
}
