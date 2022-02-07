package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Event;
import eu.europeana.metis.sandbox.domain.Record;
import java.util.Set;
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
        "createdQueue","transformationEdmExternalQueue", taskExecutor);
  }

  @Test
  void publishWithoutXslt_expectSuccess() throws Exception {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L)
                        .europeanaId("1")
        .country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L)
                        .europeanaId("2")
        .country(Country.ITALY).language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    service.publishWithoutXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("createdQueue"), any(Event.class));
  }

  @Test
  void publishWithoutXslt_asyncFail_expectNoFail() throws ExecutionException, InterruptedException {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L)
        .europeanaId("1")
        .country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2")
        .country(Country.ITALY).language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
        .convertAndSend(anyString(), any(Event.class));

    service.publishWithoutXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("createdQueue"), any(Event.class));
  }

  @Test
  void publishWithoutXslt_nullDataset_expectFail() {
    assertThrows(NullPointerException.class, () -> service.publishWithoutXslt(null));
  }

  @Test
  void publishWithoutXslt_emptyRecords_expectFail() {
    Dataset dataset = new Dataset("1234", Set.of(), 0);
    assertThrows(IllegalArgumentException.class, () -> service.publishWithoutXslt(dataset));
  }

  @Test
  void publishWithXslt_expectSuccess() throws Exception {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L)
            .europeanaId("1")
            .country(Country.ITALY)
            .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L)
            .europeanaId("2")
            .country(Country.ITALY).language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    service.publishWithXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("transformationEdmExternalQueue"), any(Event.class));
  }

  @Test
  void publishWithXslt_asyncFail_expectNoFail() throws ExecutionException, InterruptedException {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L)
            .europeanaId("1")
            .country(Country.ITALY)
            .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2")
            .country(Country.ITALY).language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
            .convertAndSend(anyString(), any(Event.class));

    service.publishWithXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("transformationEdmExternalQueue"), any(Event.class));
  }

  @Test
  void publishWithXslt_nullDataset_expectFail() {
    assertThrows(NullPointerException.class, () -> service.publishWithXslt(null));
  }

  @Test
  void publishWithXslt_emptyRecords_expectFail() {
    Dataset dataset = new Dataset("1234", Set.of(), 0);
    assertThrows(IllegalArgumentException.class, () -> service.publishWithXslt(dataset));
  }
}