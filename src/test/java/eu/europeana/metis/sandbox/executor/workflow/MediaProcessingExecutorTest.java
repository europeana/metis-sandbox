package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.MediaProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class MediaProcessingExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private MediaProcessingService service;

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;

  @InjectMocks
  private MediaProcessingExecutor consumer;

  @Test
  void processMedia_expectSuccess() {
    Record record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(record),
        Step.ENRICH, Status.SUCCESS, 1000, "", "", "", null);
    when(service.processMedia(record)).thenReturn(new RecordInfo(record));
    consumer.processMedia(recordRecordProcessEvent);

    verify(service).processMedia(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.MEDIA_PROCESS, captor.getValue().getStep());
  }

  @Test
  void processMedia_serviceThrowException_expectFailStatus() {
    Record record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(record),
        Step.ENRICH, Status.SUCCESS, 1000, "", "", "", null);

    when(service.processMedia(record))
        .thenThrow(new RecordProcessingException("1", new Exception()));

    consumer.processMedia(recordRecordProcessEvent);

    verify(service).processMedia(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }
}