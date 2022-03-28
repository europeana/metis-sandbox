package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import nl.altindag.log.LogCaptor;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class CloseExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;

  @InjectMocks
  private CloseExecutor consumer;

  @Test
  void close_expectSuccess() {
    var record = getTestRecord();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_ZIP, Status.SUCCESS);

    consumer.close(recordEvent);

    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.CLOSE, captor.getValue().getStep());
  }

  @Test
  void close_inputMessageWithFailStatus_expectNoInteractions() {
    var record = getTestRecord();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_ZIP, Status.FAIL);

    consumer.close(recordEvent);

    verify(amqpTemplate, never()).convertAndSend(any(), any(RecordProcessEvent.class));
  }

  @Test
  void close_exception_expectLogError() {
    final LogCaptor logCaptor = LogCaptor.forClass(CloseExecutor.class);
    var record = getTestRecord();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_ZIP, Status.SUCCESS);
    final RuntimeException runtimeException = new AmqpException("Queue Failure");
    doThrow(runtimeException)
        .when(amqpTemplate)
        .convertAndSend(any(), any(Object.class));

    consumer.close(recordEvent);

    assertLogCaptor(logCaptor);
  }

  private Record getTestRecord() {
    return Record.builder()
                 .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT)
                 .content("".getBytes())
                 .recordId(1L).build();
  }

  private void assertLogCaptor(LogCaptor logCaptor) {
    assertEquals(1, logCaptor.getErrorLogs().size());
    final String testMessage = logCaptor.getErrorLogs().stream().findFirst().get();
    assertTrue(testMessage.contains("Close executor error"));
  }
}