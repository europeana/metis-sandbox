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
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.InternalValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class InternalValidationExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private InternalValidationService service;

  @Captor
  private ArgumentCaptor<Event> captor;

  @InjectMocks
  private InternalValidationExecutor consumer;

  @Test
  void validateInternal_expectSuccess() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new Event(new RecordInfo(record), Step.VALIDATE_INTERNAL, Status.SUCCESS);

    when(service.validate(record)).thenReturn(new RecordInfo(record));
    consumer.validateInternal(recordEvent);

    verify(service).validate(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.VALIDATE_INTERNAL, captor.getValue().getStep());
  }

  @Test
  void validateInternal_inputMessageWithFailStatus_expectNoInteractions() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new Event(new RecordInfo(record), Step.VALIDATE_INTERNAL, Status.FAIL);

    consumer.validateInternal(recordEvent);

    verify(service, never()).validate(record);
    verify(amqpTemplate, never()).convertAndSend(any(), any(Event.class));
  }

  @Test
  void validateInternal_serviceThrowException_expectFailStatus() {
    var record = Record.builder()
        .datasetId("1").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId(1L).build();
    var recordEvent = new Event(new RecordInfo(record), Step.VALIDATE_INTERNAL, Status.SUCCESS);

    when(service.validate(record)).thenThrow(new RecordProcessingException("1", new Exception()));

    consumer.validateInternal(recordEvent);

    verify(service).validate(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }
}