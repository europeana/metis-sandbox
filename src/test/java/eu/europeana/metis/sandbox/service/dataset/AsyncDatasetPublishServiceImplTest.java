package eu.europeana.metis.sandbox.service.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Dataset;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class AsyncDatasetPublishServiceImplTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  @Mock
  private HarvestService harvestService;

  @Mock
  private DatasetService datasetService;

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;

  @Mock
  private OaiHarvester oaiHarvester;

  private final Executor taskExecutor = Runnable::run;

  private AsyncDatasetPublishService service;


  @BeforeEach
  void setUp() {
    service = new AsyncDatasetPublishServiceImpl(amqpTemplate, "createdQueue", "transformationEdmExternalQueue", taskExecutor,
        oaiHarvester, harvestService, datasetService, 10);
  }

  @Test
  void publishWithoutXslt_expectSuccess() throws Exception {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    service.publishWithoutXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("createdQueue"), any(RecordProcessEvent.class));
  }

  @Test
  void publishWithoutXslt_asyncFail_expectNoFail() throws ExecutionException, InterruptedException {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
        .convertAndSend(anyString(), any(RecordProcessEvent.class));

    service.publishWithoutXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("createdQueue"), any(RecordProcessEvent.class));
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

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    service.publishWithXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("transformationEdmExternalQueue"), any(RecordProcessEvent.class));
  }

  @Test
  void publishWithXslt_asyncFail_expectNoFail() throws ExecutionException, InterruptedException {

    var record1 = Record.builder().datasetId("1").datasetName("").recordId(1L).europeanaId("1").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();
    var record2 = Record.builder().datasetId("1").datasetName("").recordId(2L).europeanaId("2").country(Country.ITALY)
        .language(Language.IT).content("".getBytes()).build();

    Dataset dataset = new Dataset("1234", Set.of(record1, record2), 0);

    doThrow(new AmqpException("Issue publishing this record")).when(amqpTemplate)
        .convertAndSend(anyString(), any(RecordProcessEvent.class));

    service.publishWithXslt(dataset).get();

    verify(amqpTemplate, times(2)).convertAndSend(eq("transformationEdmExternalQueue"), any(RecordProcessEvent.class));
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

  @Test
  void runHarvestOaiAsync_withoutXslt_expectSuccess() throws HarvesterException {
    Record recordData = Record.builder().country(Country.NETHERLANDS).language(Language.NL).datasetName("datasetName")
        .datasetId("datasetId").content(new byte[0]).build();

    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(iteratorList);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(datasetService.isXsltPresent(anyString())).thenReturn(false);
    when(harvestService.harvestOaiRecordHeader(anyString(), any(OaiHarvestData.class),
        any(Record.RecordBuilder.class))).thenReturn(new RecordInfo(recordData));

    service.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, oaiHarvestData);
    verify(amqpTemplate, times(1)).convertAndSend(eq("createdQueue"), captor.capture());
    assertEquals(recordData, captor.getValue().getRecord());
  }

  @Test
  void runHarvestOaiAsync_withoutXsltReachMaxRecords_expectSuccess() throws HarvesterException {
    service = new AsyncDatasetPublishServiceImpl(amqpTemplate, "createdQueue", "transformationEdmExternalQueue", taskExecutor,
        oaiHarvester, harvestService, datasetService, 1);

    Record recordData = Record.builder().country(Country.NETHERLANDS).language(Language.NL).datasetName("datasetName")
        .datasetId("datasetId").content(new byte[0]).build();

    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
    OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    iteratorList.add(element2);
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(iteratorList);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(datasetService.isXsltPresent(anyString())).thenReturn(false);
    when(harvestService.harvestOaiRecordHeader(anyString(), any(OaiHarvestData.class),
        any(Record.RecordBuilder.class))).thenReturn(new RecordInfo(recordData));

    service.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, oaiHarvestData);

    verify(datasetService, times(1)).updateNumberOfTotalRecord("datasetId", 1);
    verify(datasetService, times(1)).updateRecordsLimitExceededToTrue("datasetId");
    verify(amqpTemplate, times(1)).convertAndSend(eq("createdQueue"), captor.capture());
    assertEquals(recordData, captor.getValue().getRecord());
  }

  @Test
  void runHarvestOaiAsync_withXslt_expectSuccess() throws HarvesterException {
    Record recordData = Record.builder().country(Country.NETHERLANDS).language(Language.NL).datasetName("datasetName")
        .datasetId("datasetId").content(new byte[0]).build();

    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(iteratorList);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(datasetService.isXsltPresent(anyString())).thenReturn(true);
    when(harvestService.harvestOaiRecordHeader(anyString(), any(OaiHarvestData.class),
        any(Record.RecordBuilder.class))).thenReturn(new RecordInfo(recordData));

    service.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, oaiHarvestData);
    verify(amqpTemplate, times(1)).convertAndSend(eq("transformationEdmExternalQueue"), captor.capture());
    assertEquals(recordData, captor.getValue().getRecord());
  }

  @Test
  void runHarvestOaiAsync_withXsltReachMaxRecords_expectSuccess() throws HarvesterException {
    service = new AsyncDatasetPublishServiceImpl(amqpTemplate, "createdQueue", "transformationEdmExternalQueue", taskExecutor,
        oaiHarvester, harvestService, datasetService, 1);

    Record recordData = Record.builder().country(Country.NETHERLANDS).language(Language.NL).datasetName("datasetName")
        .datasetId("datasetId").content(new byte[0]).build();

    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
    OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    iteratorList.add(element2);
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(iteratorList);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(datasetService.isXsltPresent(anyString())).thenReturn(true);
    when(harvestService.harvestOaiRecordHeader(anyString(), any(OaiHarvestData.class),
        any(Record.RecordBuilder.class))).thenReturn(new RecordInfo(recordData));

    service.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, oaiHarvestData);

    verify(datasetService, times(1)).updateNumberOfTotalRecord("datasetId", 1);
    verify(datasetService, times(1)).updateRecordsLimitExceededToTrue("datasetId");
    verify(amqpTemplate, times(1)).convertAndSend(eq("transformationEdmExternalQueue"), captor.capture());
    assertEquals(recordData, captor.getValue().getRecord());
  }

  @Test
  void runHarvestOaiAsync_withoutXsltSkipDeletedRecords_expectSuccess() throws HarvesterException {
    service = new AsyncDatasetPublishServiceImpl(amqpTemplate, "createdQueue", "transformationEdmExternalQueue", taskExecutor,
        oaiHarvester, harvestService, datasetService, 1);

    Record recordData = Record.builder().country(Country.NETHERLANDS).language(Language.NL).datasetName("datasetName")
        .datasetId("datasetId").content(new byte[0]).build();

    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", true, Instant.now());
    OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    iteratorList.add(element2);
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(iteratorList);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(datasetService.isXsltPresent(anyString())).thenReturn(false);
    when(harvestService.harvestOaiRecordHeader(anyString(), any(OaiHarvestData.class),
        any(Record.RecordBuilder.class))).thenReturn(new RecordInfo(recordData));

    service.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, oaiHarvestData);

    verify(datasetService, times(1)).updateNumberOfTotalRecord("datasetId", 1);
    verify(datasetService, times(0)).updateRecordsLimitExceededToTrue("datasetId");
    verify(amqpTemplate, times(1)).convertAndSend(eq("createdQueue"), captor.capture());
    assertEquals(recordData, captor.getValue().getRecord());
  }

  @Test
  void runHarvestOaiAsync_withXsltSkipDeletedRecords_expectSuccess() throws HarvesterException {
    service = new AsyncDatasetPublishServiceImpl(amqpTemplate, "createdQueue", "transformationEdmExternalQueue", taskExecutor,
        oaiHarvester, harvestService, datasetService, 1);

    Record recordData = Record.builder().country(Country.NETHERLANDS).language(Language.NL).datasetName("datasetName")
        .datasetId("datasetId").content(new byte[0]).build();

    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", true, Instant.now());
    OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    iteratorList.add(element2);
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(iteratorList);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(datasetService.isXsltPresent(anyString())).thenReturn(true);
    when(harvestService.harvestOaiRecordHeader(anyString(), any(OaiHarvestData.class),
        any(Record.RecordBuilder.class))).thenReturn(new RecordInfo(recordData));

    service.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, oaiHarvestData);

    verify(datasetService, times(1)).updateNumberOfTotalRecord("datasetId", 1);
    verify(datasetService, times(0)).updateRecordsLimitExceededToTrue("datasetId");
    verify(amqpTemplate, times(1)).convertAndSend(eq("transformationEdmExternalQueue"), captor.capture());
    assertEquals(recordData, captor.getValue().getRecord());
  }

  private static class TestHeaderIterator implements OaiRecordHeaderIterator {

    private final List<OaiRecordHeader> source;

    private TestHeaderIterator(List<OaiRecordHeader> source) {
      this.source = source;
    }

    @Override
    public void forEachFiltered(final ReportingIteration<OaiRecordHeader> action, final Predicate<OaiRecordHeader> filter) {
      this.source.forEach(action::process);
    }

    @Override
    public void close() {
    }
  }

}