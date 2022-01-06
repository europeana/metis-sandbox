package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.IndexEnvironment;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.exception.RecordProcessingException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.service.workflow.IndexingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class PreviewedConsumerTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private IndexingService service;

  @Captor
  private ArgumentCaptor<Event> captor;

  @InjectMocks
  private PublishExecutor consumer;

  @Test
  void publish_expectSuccess() {
    var record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId("").build();
    var recordEvent = new Event(new RecordInfo(record), Step.CREATE, Status.SUCCESS);

    when(service.index(record, IndexEnvironment.PUBLISH)).thenReturn(new RecordInfo(record));
    consumer.publish(recordEvent);

    verify(service).index(record, IndexEnvironment.PUBLISH);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.PUBLISH, captor.getValue().getStep());
  }

  @Test
  void publish_inputMessageWithFailStatus_expectNoInteractions() {
    var record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId("").build();
    var recordEvent = new Event(new RecordInfo(record), Step.CREATE, Status.FAIL);

    consumer.publish(recordEvent);

    verify(service, never()).index(record, IndexEnvironment.PUBLISH);
    verify(amqpTemplate, never()).convertAndSend(any(), any(Event.class));
  }

  @Test
  void publish_serviceThrowException_expectFailStatus() {
    var record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT)
        .content("".getBytes())
        .recordId("").build();
    var recordEvent = new Event(new RecordInfo(record), Step.CREATE, Status.SUCCESS);

    when(service.index(record, IndexEnvironment.PUBLISH))
        .thenThrow(new RecordProcessingException("1", new Exception()));

    consumer.publish(recordEvent);

    verify(service).index(record, IndexEnvironment.PUBLISH);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }
}