package eu.europeana.metis.sandbox.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.record.RecordLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventRecordLogConsumerTest {

  @Mock
  private RecordLogService recordLogService;

  @InjectMocks
  private EventRecordLogConsumer consumer;

  @Test
  void logRecord_expectSuccess() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST, Status.SUCCESS);

    consumer.logRecord(recordEvent);

    verify(recordLogService).logRecordEvent(any(RecordProcessEvent.class));
  }

  @Test
  void logRecord_logError_expectFail() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST, Status.SUCCESS);

    doThrow(new RecordProcessingException("1", new Exception())).when(recordLogService)
        .logRecordEvent(any(RecordProcessEvent.class));
    assertThrows(RecordProcessingException.class, () -> consumer.logRecord(recordEvent));

    verify(recordLogService).logRecordEvent(any(RecordProcessEvent.class));
  }
}