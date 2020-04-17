package eu.europeana.metis.sandbox.consumer.workflow;

import static org.junit.jupiter.api.Assertions.*;
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
import eu.europeana.metis.sandbox.domain.EventError;
import eu.europeana.metis.sandbox.domain.Record;
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
class EnrichedConsumerTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private MediaProcessingService service;

  @Captor
  private ArgumentCaptor<Event> captor;

  @InjectMocks
  private EnrichedConsumer consumer;

  @Test
  void processMedia_expectSuccess() {
    Record record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT).content("")
        .recordId("").build();
    Event recordEvent = new Event(record, Step.ENRICH);

    when(service.processMedia(record)).thenReturn(record);
    consumer.processMedia(recordEvent);

    verify(service).processMedia(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.MEDIA_PROCESS, captor.getValue().getStep());
  }

  @Test
  void processMedia_serviceThrowException_expectFailStatus() {
    Record record = Record.builder()
        .datasetId("").datasetName("").country(Country.ITALY).language(Language.IT).content("")
        .recordId("").build();
    Event recordEvent = new Event(record, Step.ENRICH);

    when(service.processMedia(record)).thenThrow(new RecordProcessingException("1", new Exception()));

    consumer.processMedia(recordEvent);

    verify(service).processMedia(record);
    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Status.FAIL, captor.getValue().getStatus());
  }
}