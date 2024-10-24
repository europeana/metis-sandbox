package eu.europeana.metis.sandbox.service.debias;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class RecordPublishDeBiasQueueServiceTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  private RecordPublishDeBiasQueueService service;

  @BeforeEach
  void setUp() {
    service = new RecordPublishDeBiasQueueService(amqpTemplate, "deBiasReadyQueue");
  }

  @Test
  void publishToDeBiasQueue() {
    var testRecord = Record.builder()
                           .datasetId("1")
                           .datasetName("")
                           .recordId(1L)
                           .europeanaId("1")
                           .country(Country.NETHERLANDS)
                           .language(Language.NL)
                           .content("content".getBytes())
                           .build();
    service.publishToDeBiasQueue(new RecordInfo(testRecord));
    verify(amqpTemplate, times(1))
        .convertAndSend(eq("deBiasReadyQueue"), any(RecordProcessEvent.class));
  }

  @Test
  void publishToDeBiasQueueException() {
    doThrow(new AmqpException("error in queue")).when(amqpTemplate)
                                                .convertAndSend(eq("deBiasReadyQueue"), any(RecordProcessEvent.class));
    var testRecord = Record.builder()
                           .datasetId("1")
                           .datasetName("")
                           .recordId(1L)
                           .europeanaId("1")
                           .country(Country.NETHERLANDS)
                           .language(Language.NL)
                           .content("content".getBytes())
                           .build();
    service.publishToDeBiasQueue(new RecordInfo(testRecord));
    verify(amqpTemplate, times(1))
        .convertAndSend(eq("deBiasReadyQueue"), any(RecordProcessEvent.class));
  }
}
