package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    var record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), "1", Step.CREATE, Status.SUCCESS,
        1000, "", "", "", null);

    consumer.close(recordEvent);

    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.CLOSE, captor.getValue().getStep());
  }

  @Test
  void close_inputMessageWithFailStatus_expectNoInteractions() {
    var record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), "1", Step.CREATE, Status.FAIL, 1000,
        "", "", "", null);

    consumer.close(recordEvent);

    verify(amqpTemplate, never()).convertAndSend(any(), any(RecordProcessEvent.class));
  }
}