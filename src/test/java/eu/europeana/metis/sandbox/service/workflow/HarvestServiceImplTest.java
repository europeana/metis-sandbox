package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.exception.StepIsTooBigException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import eu.europeana.metis.sandbox.service.dataset.RecordPublishService;
import eu.europeana.metis.utils.CompressedFileExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class HarvestServiceImplTest {

    @Mock
    private HttpHarvester httpHarvester;

    @Mock
    private OaiHarvester oaiHarvester;

    @Mock
    private RecordPublishService recordPublishService;

    @Mock
    private DatasetService datasetService;

    private HarvestService harvestService;

    @Captor
    private ArgumentCaptor<RecordInfo> captorRecordInfo;

    @Mock
    private RecordRepository recordRepository;

    @BeforeEach
    void setUp() {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1000,
                recordRepository);
    }

    @Test
    void harvest_notExceedingRecordLimitWithoutXslt_expectSuccess() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithoutXslt(recordPublishService, 2, Step.HARVEST_FILE, 2L);
    }

    @Test
    void harvest_exceedingRecordLimitWithoutXslt_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
                recordRepository);

        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithoutXslt(recordPublishService, 1, Step.HARVEST_FILE, 1L);
    }

    @Test
    void harvest_withStepSize_expectSuccess() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIteratorStepSizeTest());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), 2,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithoutXslt(recordPublishService, 2, Step.HARVEST_FILE, 2L);
    }

    @Test
    void harvest_withStepSize_expectFail() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIteratorStepSizeTest());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        assertThrows(StepIsTooBigException.class, ()->
            harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), 10,
                    CompressedFileExtension.ZIP));
    }

    @Test
    void harvest_notExceedingRecordLimitWithXslt_expectSuccess() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithXslt(recordPublishService, 2, Step.HARVEST_FILE, 2L);
    }

    @Test
    void harvest_withStepSizeAndWithXslt_expectSuccess() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIteratorStepSizeTest());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), 2,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithXslt(recordPublishService, 2, Step.HARVEST_FILE, 2L);
    }

    @Test
    void harvest_exceedingRecordLimitWithXslt_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
                recordRepository);

        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithXslt(recordPublishService, 1, Step.HARVEST_FILE, 1L);
    }

    @Test
    void harvest_failsWithErrorMessage_expectSuccess() throws HarvesterException {
        Path record1Path = Paths.get("src", "test", "resources", "zip", "Record1.xml");
        assertTrue(Files.exists(record1Path));
        List<Path> pathList = new ArrayList<>();
        pathList.add(record1Path);

        HttpRecordIterator httpRecordIterator = new TestUtils.TestHttpRecordIterator(pathList);

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpRecordIterator);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1);

        Record.RecordBuilder recordBuilderToTest = spy(Record.builder().datasetName("datasetName").datasetId("datasetId")
                .language(Language.NL).country(Country.NETHERLANDS));

        when(recordBuilderToTest.build()).thenThrow(RuntimeException.class)
                .thenReturn(Record.builder().datasetName("datasetName").datasetId("datasetId")
                        .language(Language.NL).country(Country.NETHERLANDS).build());

        harvestService.harvest(new ByteArrayInputStream("inputStream".getBytes(StandardCharsets.UTF_8)), "datasetId",
                recordBuilderToTest, null, CompressedFileExtension.ZIP);
        verify(recordPublishService, times(0)).publishToHarvestQueue(captorRecordInfo.capture(), any(Step.class));
        verify(recordRepository, times(2)).save(any(RecordEntity.class));
    }

    @Test
    void harvest_expectFail() throws HarvesterException {
        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenThrow(
                HarvesterException.class);

        assertThrows(ServiceException.class,
                () -> harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                        CompressedFileExtension.ZIP));
    }

    @Test
    void harvest_duplicatedById_expectSuccess() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(
                addDuplicatedRecordsToHttpIterator(prepareMockListForHttpIterator()));

        RecordEntity recordEntity1 = new RecordEntity("", "src/test/resources/zip/Record1.xml", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "src/test/resources/zip/Record2.xml", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2);
        when(recordRepository.findByProviderIdAndDatasetId(
                generateHarvestProviderIdFromTemporaryPath("src/test/resources/zip/Record2.xml"), "datasetId"))
                .thenReturn(null)
                .thenReturn(recordEntity2);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithoutXslt(recordPublishService, 3, Step.HARVEST_FILE, 3L);
    }

    @Test
    void harvest_duplicatedByContent_expectSuccess() throws HarvesterException {
        HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(
                addDuplicatedRecordsToHttpIterator(prepareMockListForHttpIterator()));

        RecordEntity recordEntity1 = new RecordEntity("", "src/test/resources/zip/Record1.xml", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "src/test/resources/zip/Record2.xml", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);

        when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
                httpIterator);
        when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                .thenReturn(recordEntity2);
        when(recordRepository.findByProviderIdAndDatasetId(eq(
                generateHarvestProviderIdFromTemporaryPath("src/test/resources/zip/Record2.xml")
        ), eq("datasetId")))
                .thenReturn(null)
                .thenReturn(recordEntity2);

        harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord(), null,
                CompressedFileExtension.ZIP);

        assertHarvestProcessWithoutXslt(recordPublishService, 3, Step.HARVEST_FILE, 3L);
    }

    @Test
    void harvestOaiPmh_notExceedingLimitWithoutXslt_expectSuccess() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1).thenReturn(recordEntity2);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        assertHarvestProcessWithoutXslt(recordPublishService, 2, Step.HARVEST_OAI_PMH, 2L);
    }

    @Test
    void harvestOaiPmh_exceedingLimitWithoutXslt_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        assertHarvestProcessWithoutXslt(recordPublishService, 1, Step.HARVEST_OAI_PMH, 1L);
    }

    @Test
    void harvestOaiPmh_withStepSize_expectSuccess() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIteratorStepSizeTest());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class)))
                .thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, 2);

        assertHarvestProcessWithoutXslt(recordPublishService, 2, Step.HARVEST_OAI_PMH, 2L);
    }

    @Test
    void harvestOaiPmh_withStepSize_expectFail() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIteratorStepSizeTest());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class)))
                .thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        assertThrows(StepIsTooBigException.class, () ->
            harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, 10));
    }

    @Test
    void harvestOaiPmh_notExceedingLimitWithXslt_expectSuccess() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1).thenReturn(recordEntity2);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        assertHarvestProcessWithXslt(recordPublishService, 2, Step.HARVEST_OAI_PMH, 2L);
    }

    @Test
    void harvestOaiPmh_exceedingLimitWithXslt_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        assertHarvestProcessWithXslt(recordPublishService, 1, Step.HARVEST_OAI_PMH, 1L);
    }

    @Test
    void harvestOaiPmh_withStepSizeAndXslt_expectSuccess() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIteratorStepSizeTest());

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        RecordEntity recordEntity3 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity3.setId(3L);
        RecordEntity recordEntity4 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity4.setId(4L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        when(datasetService.isXsltPresent(anyString())).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class)))
                .thenReturn(recordEntity1)
                .thenReturn(recordEntity2)
                .thenReturn(recordEntity3)
                .thenReturn(recordEntity4);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, 2);

        assertHarvestProcessWithXslt(recordPublishService, 2, Step.HARVEST_OAI_PMH, 2L);
    }

    @Test
    void harvestOaiPmh_expectFail() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenThrow(HarvesterException.class);
        assertThrows(ServiceException.class,
                () -> harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null));
    }

    @Test
    void harvestOaiPmh_failsWithErrorMessage_expectSuccess() throws HarvesterException {
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        final OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
        final List<OaiRecordHeader> iteratorList = new ArrayList<>();
        iteratorList.add(element1);

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(iteratorList);

        RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity1.setId(1L);

        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1);

        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenThrow(HarvesterException.class);
        Record.RecordBuilder recordBuilderToTest = spy(Record.builder().datasetName("datasetName").datasetId("datasetId")
                .language(Language.NL).country(Country.NETHERLANDS));

        harvestService.harvestOaiPmh("datasetId", recordBuilderToTest, oaiHarvestData, null);

        verify(recordPublishService, times(0)).publishToHarvestQueue(captorRecordInfo.capture(), any(Step.class));
        verify(recordRepository, times(1)).save(any(RecordEntity.class));
    }

    @Test
    void runHarvestOaiAsync_withoutXsltSkipDeletedRecords_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 2,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(
                addDeletedRecordToListOaiRecordIterator(
                        prepareListForOaiRecordIterator(),
                        "oaiIdentifier3"));

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        verify(datasetService, times(1)).setRecordLimitExceeded("datasetId");

        assertHarvestProcessWithoutXslt(recordPublishService, 2, Step.HARVEST_OAI_PMH, 2L);
    }

    @Test
    void runHarvestOaiAsync_withoutXsltSkipDeletedRecordsWithStepSize_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 2,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(
                addDeletedRecordToListOaiRecordIterator(addDeletedRecordToListOaiRecordIterator(
                        prepareListForOaiRecordIterator(),
                        "oaiIdentifier3"),"oaiIdentifier4"));

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.save(any(RecordEntity.class)))
                .thenReturn(recordEntity)
                .thenReturn(recordEntity2);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, 2);

        verify(datasetService, times(0)).setRecordLimitExceeded("datasetId");

        assertHarvestProcessWithoutXslt(recordPublishService, 1, Step.HARVEST_OAI_PMH, 1L);
    }

    @Test
    void runHarvestOaiAsync_withXsltSkipDeletedRecords_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 2,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(
                addDeletedRecordToListOaiRecordIterator(
                        prepareListForOaiRecordIterator(),
                        "oaiIdentifier3"));

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(datasetService.isXsltPresent(anyString())).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        verify(datasetService, times(1)).setRecordLimitExceeded("datasetId");

        assertHarvestProcessWithXslt(recordPublishService, 2, Step.HARVEST_OAI_PMH, 2L);
    }

    @Test
    void runHarvestOaiAsync_withXsltSkipDeletedRecordsAndStepSize_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 2,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(
                addDeletedRecordToListOaiRecordIterator(addDeletedRecordToListOaiRecordIterator(
                        prepareListForOaiRecordIterator(),
                        "oaiIdentifier3"),"oaiIdentifier4"));

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);
        RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity2.setId(2L);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(datasetService.isXsltPresent(anyString())).thenReturn(true);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity).thenReturn(recordEntity2);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, 2);

        verify(datasetService, times(0)).setRecordLimitExceeded("datasetId");

        assertHarvestProcessWithXslt(recordPublishService, 1, Step.HARVEST_OAI_PMH, 1L);
    }

    @Test
    void harvestOaiPmh_duplicatedById_expectSuccess() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 5,
                recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

        OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(
                addDuplicatedRecordsToListOaiRecordIterator(
                        prepareListForOaiRecordIterator()));

        OaiRecord mockOaiRecord = mock(OaiRecord.class);
        when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record1".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(new ByteArrayInputStream("record2".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(new ByteArrayInputStream("record1".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(new ByteArrayInputStream("record2".getBytes(StandardCharsets.UTF_8)))
                .thenReturn(new ByteArrayInputStream("record3".getBytes(StandardCharsets.UTF_8)));
        RecordEntity recordEntity = new RecordEntity("", "", "datasetId", "", "", "", "", "", "", "");
        recordEntity.setId(1L);
        when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
        when(datasetService.isXsltPresent(anyString())).thenReturn(false);
        when(recordRepository.findByProviderIdAndDatasetId("oaiIdentifier1", "datasetId"))
                .thenReturn(null)
                .thenReturn(recordEntity);
        when(recordRepository.findByProviderIdAndDatasetId("oaiIdentifier2", "datasetId"))
                .thenReturn(null)
                .thenReturn(recordEntity);
        when(recordRepository.findByProviderIdAndDatasetId("oaiIdentifier3", "datasetId"))
                .thenReturn(null);

        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

        harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData, null);

        verify(datasetService, times(0)).setRecordLimitExceeded("datasetId");

        assertHarvestProcessWithoutXslt(recordPublishService, 5, Step.HARVEST_OAI_PMH, 5L);
    }

    private void assertHarvestProcess(RecordPublishService recordPublishService, int times, Step step,
                                      Long numberOfRecords) {
        verify(datasetService).updateNumberOfTotalRecord(eq("datasetId"), eq(numberOfRecords));
        assertTrue(captorRecordInfo.getAllValues().stream().allMatch(x -> x.getRecord().getDatasetId().equals("datasetId")));
        assertTrue(captorRecordInfo.getAllValues().stream().allMatch(x -> x.getRecord().getContent() != null));
        assertEquals(Country.NETHERLANDS, captorRecordInfo.getValue().getRecord().getCountry());
        assertEquals(Language.NL, captorRecordInfo.getValue().getRecord().getLanguage());
        assertEquals("datasetName", captorRecordInfo.getValue().getRecord().getDatasetName());
    }

    private void assertHarvestProcessWithXslt(RecordPublishService recordPublishService, int times, Step step,
                                              Long numberOfRecords) {
        verify(recordPublishService, times(times)).publishToTransformationToEdmExternalQueue(captorRecordInfo.capture(), eq(step));
        assertHarvestProcess(recordPublishService, times, step, numberOfRecords);
    }

    private void assertHarvestProcessWithoutXslt(RecordPublishService recordPublishService, int times, Step step,
                                                 Long numberOfRecords) {
        verify(recordPublishService, times(times)).publishToHarvestQueue(captorRecordInfo.capture(), eq(step));
        assertHarvestProcess(recordPublishService, times, step, numberOfRecords);
    }

    private List<Path> prepareMockListForHttpIterator() {
        Path record1Path = Paths.get("src", "test", "resources", "zip", "Record1.xml");
        assertTrue(Files.exists(record1Path));
        Path record2Path = Paths.get("src", "test", "resources", "zip", "Record2.xml");
        assertTrue(Files.exists(record2Path));
        List<Path> pathList = new ArrayList<>();
        pathList.add(record1Path);
        pathList.add(record2Path);

        return pathList;
    }

    private List<Path> prepareMockListForHttpIteratorStepSizeTest() {
        Path record1Path = Paths.get("src", "test", "resources", "zip", "Record1.xml");
        assertTrue(Files.exists(record1Path));
        Path record2Path = Paths.get("src", "test", "resources", "zip", "Record2.xml");
        assertTrue(Files.exists(record2Path));
        Path record3Path = Paths.get("src", "test", "resources", "zip", "Record3.xml");
        assertTrue(Files.exists(record3Path));
        Path record4Path = Paths.get("src", "test", "resources", "zip", "Record4.xml");
        assertTrue(Files.exists(record4Path));
        List<Path> pathList = new ArrayList<>();
        pathList.add(record1Path);
        pathList.add(record2Path);
        pathList.add(record3Path);
        pathList.add(record4Path);

        return pathList;

    }

    private List<Path> addDuplicatedRecordsToHttpIterator(final List<Path> pathList) {
        final Path record2Path = Paths.get("src", "test", "resources", "zip", "Record2.xml");
        assertTrue(Files.exists(record2Path));
        pathList.add(record2Path);
        return pathList;
    }

    private Record.RecordBuilder createMockEncapsulatedRecord() {
        return Record.builder()
                .datasetId("datasetId")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .content(new byte[0])
                .datasetName("datasetName");
    }

    private List<OaiRecordHeader> prepareListForOaiRecordIterator() {
        final OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
        final OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
        final List<OaiRecordHeader> iteratorList = new ArrayList<>();
        iteratorList.add(element1);
        iteratorList.add(element2);
        return iteratorList;
    }

    private List<OaiRecordHeader> prepareListForOaiRecordIteratorStepSizeTest() {
        final OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
        final OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
        final OaiRecordHeader element3 = new OaiRecordHeader("oaiIdentifier3", false, Instant.now());
        final OaiRecordHeader element4 = new OaiRecordHeader("oaiIdentifier4", false, Instant.now());
        List<OaiRecordHeader> pathList = new ArrayList<>();
        pathList.add(element1);
        pathList.add(element2);
        pathList.add(element3);
        pathList.add(element4);

        return pathList;
    }

    private List<OaiRecordHeader> addDeletedRecordToListOaiRecordIterator(final List<OaiRecordHeader> iteratorList, String oaiIdentifier) {
        final OaiRecordHeader element = new OaiRecordHeader(oaiIdentifier, true, Instant.now());
        iteratorList.add(element);
        return iteratorList;
    }

    private List<OaiRecordHeader> addDuplicatedRecordsToListOaiRecordIterator(final List<OaiRecordHeader> iteratorList) {
        final Instant datestamp = Instant.now();
        final OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, datestamp);
        final OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, datestamp);
        final OaiRecordHeader element3 = new OaiRecordHeader("oaiIdentifier3", false, datestamp);
        iteratorList.addAll(List.of(element1, element2, element3));
        return iteratorList;
    }

    private String generateHarvestProviderIdFromTemporaryPath(String value) {
        Path path = Paths.get(value);
        int pathNameCount = path.getNameCount();
        Path tmpProviderId = pathNameCount >= 2 ? path.subpath(pathNameCount - 2, pathNameCount) : path.getFileName();
        return tmpProviderId.toString();
    }
}
