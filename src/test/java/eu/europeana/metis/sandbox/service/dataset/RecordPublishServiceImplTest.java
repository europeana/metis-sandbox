package eu.europeana.metis.sandbox.service.dataset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class RecordPublishServiceImplTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  private final Executor taskExecutor = Runnable::run;

  private RecordPublishService service;


  @BeforeEach
  void setUp() {
    service = new RecordPublishServiceImpl(amqpTemplate, "createdQueue",
        "transformationEdmExternalQueue");
  }

  @Test
  void publishWithoutXslt_expectSuccess() {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    dataset.getRecords().forEach(record -> service.publishToHarvestQueue(new RecordInfo(record), Step.HARVEST_ZIP));

    verify(amqpTemplate, times(2)).convertAndSend(eq("createdQueue"), any(RecordProcessEvent.class));
  }

  @Test
  void publishWithoutXslt_asyncFail_expectNoFail() {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
        .convertAndSend(anyString(), any(RecordProcessEvent.class));

    dataset.getRecords().forEach(record -> service.publishToHarvestQueue(new RecordInfo(record), Step.HARVEST_ZIP));

    verify(amqpTemplate, times(2)).convertAndSend(eq("createdQueue"), any(RecordProcessEvent.class));
  }

  @Test
  void publishWithXslt_expectSuccess() {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    dataset.getRecords().forEach(record -> service.publishToTransformationToEdmExternalQueue(new RecordInfo(record), Step.HARVEST_ZIP));

    verify(amqpTemplate, times(2)).convertAndSend(eq("transformationEdmExternalQueue"), any(RecordProcessEvent.class));
  }

  @Test
  void publishWithXslt_asyncFail_expectNoFail() {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
        .convertAndSend(anyString(), any(RecordProcessEvent.class));

    dataset.getRecords().forEach(record -> service.publishToTransformationToEdmExternalQueue(new RecordInfo(record), Step.HARVEST_ZIP));

    verify(amqpTemplate, times(2)).convertAndSend(eq("transformationEdmExternalQueue"), any(RecordProcessEvent.class));
  }

}
