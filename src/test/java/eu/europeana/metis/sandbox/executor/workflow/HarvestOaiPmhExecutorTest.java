package eu.europeana.metis.sandbox.executor.workflow;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.domain.RecordProcessEvent;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import eu.europeana.metis.sandbox.service.workflow.HarvestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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
    @Spy
    private HarvestOaiPmhExecutor executor;

    @BeforeEach
    void setUp(){
        executor.setRoutingKeys("sandbox.record.created", "sandbox.record.transformation.edm.external");
    }


    @Test
    void harvestOaiPmhWithoutXslt_expectSuccess()
            throws HarvesterException {

        // setup
        Record recordFromEvent = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content(new byte[0])
                .build();

        Record recordAsResult = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content("record".getBytes(StandardCharsets.UTF_8))
                .recordId(1L)
                .build();

        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadataformat");
        RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(recordFromEvent),
                Step.HARVEST_OAI_PMH, Status.SUCCESS, 1000, oaiHarvestData);
        OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier", false, Instant.now());
        List<OaiRecordHeader> iteratorList = new ArrayList<>();
        iteratorList.add(element1);
        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
                iteratorList);
        RecordInfo recordInfoResult = new RecordInfo(recordAsResult);

        when(datasetService.isXsltPresent("1")).thenReturn(0);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
                oaiRecordHeaderIterator);
        when(harvestService.harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element1))
                .thenReturn(recordInfoResult);

        executor.harvestOaiPmh(recordRecordProcessEvent);

        verify(harvestService).harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element1);
        verify(datasetService).updateNumberOfTotalRecord("1", 1);
        verify(executor).consume(eq("sandbox.record.created"), any(RecordProcessEvent.class), any(Step.class), any(Supplier.class));
        verify(amqpTemplate).convertAndSend(any(), captor.capture());
        assertEquals(Step.HARVEST_OAI_PMH, captor.getValue().getStep());
        assertEquals(recordInfoResult, captor.getValue().getRecordInfo());
        assertEquals("url", captor.getValue().getOaiHarvestData().getUrl());
        assertEquals("setspec",captor.getValue().getOaiHarvestData().getSetspec());
        assertEquals("metadataformat",captor.getValue().getOaiHarvestData().getMetadataformat());
    }

    @Test
    void harvestOaiPmhWithXslt_expectSuccess() throws HarvesterException {
        // setup
        Record recordFromEvent = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content(new byte[0])
                .build();

        Record recordAsResult = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content("record".getBytes(StandardCharsets.UTF_8))
                .recordId(1L)
                .build();

        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadataformat");
        RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(recordFromEvent),
                Step.HARVEST_OAI_PMH, Status.SUCCESS, 1000, oaiHarvestData);
        OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier", false, Instant.now());
        List<OaiRecordHeader> iteratorList = new ArrayList<>();
        iteratorList.add(element1);
        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
                iteratorList);
        RecordInfo recordInfoResult = new RecordInfo(recordAsResult);

        when(datasetService.isXsltPresent("1")).thenReturn(1);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
                oaiRecordHeaderIterator);
        when(harvestService.harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element1))
                .thenReturn(recordInfoResult);

        executor.harvestOaiPmh(recordRecordProcessEvent);

        verify(harvestService).harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element1);
        verify(datasetService).updateNumberOfTotalRecord("1", 1);
        verify(executor).consume(eq("sandbox.record.transformation.edm.external"), any(RecordProcessEvent.class), any(Step.class), any(Supplier.class));
        verify(amqpTemplate).convertAndSend(any(), captor.capture());
        assertEquals(Step.HARVEST_OAI_PMH, captor.getValue().getStep());
        assertEquals(recordInfoResult, captor.getValue().getRecordInfo());
        assertEquals("url", captor.getValue().getOaiHarvestData().getUrl());
        assertEquals("setspec",captor.getValue().getOaiHarvestData().getSetspec());
        assertEquals("metadataformat",captor.getValue().getOaiHarvestData().getMetadataformat());

    }

    @Test
    void harvestOaiPmhNumberOfIterationExceeded_expectSuccess()
            throws HarvesterException {

        // setup
        Record recordFromEvent = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content(new byte[0])
                .build();

        Record firstRecordToReturn = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content("record1".getBytes(StandardCharsets.UTF_8))
                .recordId(1L)
                .build();

        Record secondRecordToReturn = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content("record2".getBytes(StandardCharsets.UTF_8))
                .recordId(2L)
                .build();

        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadataformat");
        RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(recordFromEvent),
                Step.HARVEST_OAI_PMH, Status.SUCCESS, 2, oaiHarvestData);
        OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
        OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
        OaiRecordHeader element3 = new OaiRecordHeader("oaiIdentifier3", false, Instant.now());
        List<OaiRecordHeader> iteratorList = new ArrayList<>();
        iteratorList.add(element1);
        iteratorList.add(element2);
        iteratorList.add(element3);
        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
                iteratorList);
        RecordInfo recordInfoResult1 = new RecordInfo(firstRecordToReturn);
        RecordInfo recordInfoResult2 = new RecordInfo(secondRecordToReturn);

        // test
        when(datasetService.isXsltPresent("1")).thenReturn(0);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
                oaiRecordHeaderIterator);
        when(harvestService.harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element1))
                .thenReturn(recordInfoResult1);
        when(harvestService.harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element2))
                .thenReturn(recordInfoResult2);

        executor.harvestOaiPmh(recordRecordProcessEvent);

        verify(harvestService).harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element1);
        verify(harvestService).harvestOaiRecordHeader(oaiHarvestData, recordFromEvent, element2);
        verify(datasetService).updateNumberOfTotalRecord("1", 2);
        verify(amqpTemplate, times(2)).convertAndSend(any(), captor.capture());
        List<RecordProcessEvent> capturedEvents = captor.getAllValues();
        assertEquals(Step.HARVEST_OAI_PMH, capturedEvents.get(0).getStep());
        assertEquals(recordInfoResult1, capturedEvents.get(0).getRecordInfo());
        assertEquals(Step.HARVEST_OAI_PMH, capturedEvents.get(1).getStep());
        assertEquals(recordInfoResult2, capturedEvents.get(1).getRecordInfo());
        assertEquals(oaiHarvestData, capturedEvents.get(0).getOaiHarvestData());
        assertEquals(oaiHarvestData, capturedEvents.get(1).getOaiHarvestData());

    }

    @Test
    void harvestOaiPmh_expectFail() throws HarvesterException {
        // setup
        Record recordFromEvent = Record.builder()
                .datasetId("1")
                .datasetName("One")
                .country(Country.PORTUGAL)
                .language(Language.PT)
                .content(new byte[0])
                .build();

        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadataformat");
        RecordProcessEvent recordRecordProcessEvent = new RecordProcessEvent(new RecordInfo(recordFromEvent),
                Step.HARVEST_OAI_PMH, Status.SUCCESS, 1000, oaiHarvestData);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenThrow(HarvesterException.class);

        assertThrows(ServiceException.class, () -> executor.harvestOaiPmh(recordRecordProcessEvent));

    }


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
