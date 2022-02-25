package eu.europeana.metis.sandbox.executor.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.amqp.RecordMessageConverter;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

class StepExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;
  @Captor
  private ArgumentCaptor<Event> captor;

  @Test
  void consume() {
    amqpTemplate = mock(AmqpTemplate.class);
    StepExecutor stepExecutor = new StepExecutor(amqpTemplate);
    final String routingKey = "routing";
    final Record myTestRecord = Record.builder().recordId(1L)
                                      .datasetId("1")
                                      .datasetName("")
                                      .country(Country.FINLAND)
                                      .language(Language.FI)
                                      .content("".getBytes())
                                      .build();
    final Event myEvent = new Event(new RecordInfo(myTestRecord), Step.CREATE, Status.SUCCESS);

    stepExecutor.consume(routingKey, myEvent, Step.CREATE, () -> {
      final Record mySecondRecord = Record.builder().recordId(2L)
                                          .datasetId("1")
                                          .datasetName("")
                                          .country(Country.FINLAND)
                                          .language(Language.FI)
                                          .content("".getBytes())
                                          .build();
      final RecordInfo recordInfo = new RecordInfo(mySecondRecord);
      return recordInfo;
    });

    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.CREATE, captor.getValue().getStep());
  }
}