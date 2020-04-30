package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class AsyncDatasetPublishServiceImplTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  private final Executor taskExecutor = Runnable::run;

  private AsyncDatasetPublishService service;

  @BeforeEach
  void setUp() {
    service = new AsyncDatasetPublishServiceImpl(amqpTemplate,
        "initialQueue", taskExecutor);
  }

  @Test
  void publish_expectSuccess() throws Exception {

    var record1 = Record.builder().datasetId(1).datasetName("").recordId("1")
        .country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId(1).datasetName("").recordId("2")
        .country(Country.ITALY).language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset(1234, List.of(record1, record2));

    service.publish(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(anyString(), any(Event.class));
  }

  @Test
  void publish_asyncFail_expectNoFail() throws ExecutionException, InterruptedException {

    var record1 = Record.builder().datasetId(1).datasetName("").recordId("1")
        .country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId(1).datasetName("").recordId("2")
        .country(Country.ITALY).language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset(1234, List.of(record1, record2));

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
        .convertAndSend(anyString(), any(Event.class));

    service.publish(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(anyString(), any(Event.class));
  }

  @Test
  void publish_nullDataset_expectFail() {
    assertThrows(NullPointerException.class, () -> service.publish(null));
  }

  @Test
  void publish_emptyRecords_expectFail() {
    Dataset dataset = new Dataset(1234, List.of());
    assertThrows(IllegalArgumentException.class, () -> service.publish(dataset));
  }
}