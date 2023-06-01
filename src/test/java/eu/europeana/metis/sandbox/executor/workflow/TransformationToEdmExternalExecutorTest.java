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
import eu.europeana.metis.sandbox.service.workflow.TransformationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class TransformationToEdmExternalExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private TransformationService service;

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;

  @InjectMocks
  private TransformationToEdmExternalExecutor consumer;

  @Test
  void transformationToEdmExternal_expectSuccess() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_FILE, Status.SUCCESS);

    when(service.transform(record)).thenReturn(new RecordInfo(record));
    consumer.transformationToEdmExternal(recordEvent);

    verify(service).transform(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.TRANSFORM_TO_EDM_EXTERNAL, captor.getValue().getStep());
  }

  @Test
  void transformationToEdmExternal_inputMessageWithFailStatus_expectNoInteractions() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_FILE, Status.FAIL);

    consumer.transformationToEdmExternal(recordEvent);

    verify(service, never()).transform(record);
    verify(amqpTemplate, never()).convertAndSend(any(), any(RecordProcessEvent.class));
  }

  @Test
  void transformationToEdmExternal_serviceThrowException_expectFailStatus() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new RecordProcessEvent(new RecordInfo(record), Step.HARVEST_FILE, Status.SUCCESS);

    when(service.transform(record)).thenThrow(new RecordProcessingException("1", new Exception()));

    consumer.transformationToEdmExternal(recordEvent);

    verify(service).transform(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }
}
