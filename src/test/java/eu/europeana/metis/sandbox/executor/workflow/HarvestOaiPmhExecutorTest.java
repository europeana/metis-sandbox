package eu.europeana.metis.sandbox.executor.workflow;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Status;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;

@ExtendWith(MockitoExtension.class)
class HarvestOaiPmhExecutorTest {

  @Mock
  private AmqpTemplate amqpTemplate;

  private final Executor taskExecutor = Runnable::run;

//  private AsyncDatasetPublishService asyncDatasetPublishService;

  @InjectMocks
  private HarvestOaiPmhExecutor executor;

  @Mock
  private DatasetService datasetService;

  @Mock
  private OaiHarvestData oaiHarvestData;

//  @BeforeEach
//  void setUp() {
//    asyncDatasetPublishService = new AsyncDatasetPublishServiceImpl(amqpTemplate, "oaiHarvestQueue", "createdQueue",
//        "transformationEdmExternalQueue", taskExecutor);
//  }

  private static final OaiHarvester oaiHarvester = mock(OaiHarvester.class);

  @Captor
  private ArgumentCaptor<RecordProcessEvent> captor;


  @Test
  void harvestOaiPmh_withoutXslt_expectSuccess()
      throws HarvesterException, ExecutionException, InterruptedException {

    // setup
    Record record = Record.builder()
        .datasetId("1")
        .datasetName("One")
        .country(Country.PORTUGAL)
        .language(Language.PT)
        .content(new byte[0])
        .recordId(1L)
        .build();

    RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(record),
        Step.HARVEST_OAI_PMH, Status.SUCCESS, 1000, oaiHarvestData);

    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
        Collections.emptyList());

    // test
    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
        oaiRecordHeaderIterator);

//    asyncDatasetPublishService.harvestOaiPmh("One", "id", Country.PORTUGAL,
//        Language.PT, null, oaiHarvestData).get();

    executor.harvestOaiPmh(recordRecordProcessEvent);

    verify(amqpTemplate).convertAndSend(any(), captor.capture());

    assertEquals(Step.HARVEST_OAI_PMH, captor.getValue().getStep());
  }

  //  @Test
//  void harvestOaiPmh_expectFail()
//      throws HarvesterException, ExecutionException, InterruptedException {
//
//    // setup
//    Record record = Record.builder()
//        .datasetId("1")
//        .datasetName("One")
//        .country(Country.PORTUGAL)
//        .language(Language.PT)
//        .content("".getBytes())
//        .recordId(1L)
//        .build();
////
////    OaiHarvestData oaiHarvestData =
////        new OaiHarvestData("https://metis-repository-rest-test.eanadev.org/repository/oai",
////            "adolfo_test_24_02_2022", "edm");
//
//    RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(null,
//        Step.HARVEST_OAI_PMH, Status.SUCCESS, 1000, oaiHarvestData);
//
//    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
//        Collections.emptyList());
//
//    // test
//    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
//        oaiRecordHeaderIterator);
//
//    asyncDatasetPublishService.harvestOaiPmh("One", "id", Country.PORTUGAL,
//        Language.PT, null, oaiHarvestData).get();
//
//    consumer.harvestOaiPmh(recordRecordProcessEvent);
//
//    verify(amqpTemplate).convertAndSend(any(), captor.capture());
//
//    assertEquals(Step.HARVEST_OAI_PMH, captor.getValue().getStep());
//
//  }


  private static class TestHeaderIterator implements OaiRecordHeaderIterator {

    private final List<OaiRecordHeader> source;

    private TestHeaderIterator(List<OaiRecordHeader> source) {
      this.source = source;
    }

    @Override
    public void forEachFiltered(final ReportingIteration<OaiRecordHeader> action,
        final Predicate<OaiRecordHeader> filter) {

      this.source.forEach(action::process);
    }

    @Override
    public void close() {
    }
  }


}
