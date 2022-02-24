package eu.europeana.metis.sandbox.executor.workflow;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import eu.europeana.metis.sandbox.service.workflow.HarvestService;
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

    @Mock
    private DatasetService datasetService;

    @Mock
    private OaiHarvester oaiHarvester;

    @Mock
    private HarvestService harvestService;

    @Captor
    private ArgumentCaptor<RecordProcessEvent> captor;

    @InjectMocks
    private HarvestOaiPmhExecutor executor;


    @Test
    void harvestOaiPmh_withoutXslt_expectSuccess()
            throws HarvesterException {

        // setup
        Record recordToSend = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content(new byte[0])
                .build();

        Record recordToReturn = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content("record".getBytes(StandardCharsets.UTF_8))
                .recordId(1L)
                .build();

        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadataformat");
        RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(recordToSend),
                Step.HARVEST_OAI_PMH, Status.SUCCESS, 1000, oaiHarvestData);
        OaiRecordHeader element1 = mock(OaiRecordHeader.class);
        List<OaiRecordHeader> iteratorList = new ArrayList<>();
        iteratorList.add(element1);
        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
                iteratorList);
        RecordInfo recordInfoResult = new RecordInfo(recordToReturn);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
                oaiRecordHeaderIterator);
        when(harvestService.harvestOaiRecordHeader(oaiHarvestData, recordToSend, element1))
                .thenReturn(recordInfoResult);

        executor.harvestOaiPmh(recordRecordProcessEvent);

        verify(harvestService).harvestOaiRecordHeader(oaiHarvestData, recordToSend, element1);
        verify(datasetService).updateNumberOfTotalRecord("1", 1);
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
