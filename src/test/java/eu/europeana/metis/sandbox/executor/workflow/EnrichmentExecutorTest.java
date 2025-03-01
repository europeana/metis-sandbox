package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import eu.europeana.metis.sandbox.service.workflow.EnrichmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class EnrichmentExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private EnrichmentService service;

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;

  @InjectMocks
  private EnrichmentExecutor consumer;

  @Test
  void enrich_expectSuccess() {
    var testRecord = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(testRecord), Step.NORMALIZE, Status.SUCCESS);

    when(service.enrich(testRecord)).thenReturn(new RecordInfo(testRecord));
    consumer.enrich(recordEvent);

    verify(service).enrich(testRecord);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.ENRICH, captor.getValue().getStep());
  }

  @Test
  void enrich_inputMessageWithFailStatus_expectNoInteractions() {
    var testRecord = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(testRecord), Step.NORMALIZE, Status.FAIL);

    consumer.enrich(recordEvent);

    verify(service, never()).enrich(testRecord);
    verify(amqpTemplate, never()).convertAndSend(any(), any(RecordProcessEvent.class));
  }

  @Test
  void enrich_serviceThrowException_expectFailStatus() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.NORMALIZE, Status.SUCCESS);

    when(service.enrich(record)).thenThrow(new RecordProcessingException("1", new Exception()));

    consumer.enrich(recordEvent);

    verify(service).enrich(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }
}
