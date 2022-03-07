package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.http.CompressedFileExtension;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.http.HttpRecordIterator;
import eu.europeana.metis.harvesting.oaipmh.*;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.Step;
import eu.europeana.metis.sandbox.common.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.domain.RecordInfo;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import eu.europeana.metis.sandbox.service.dataset.RecordPublishService;
import eu.europeana.metis.sandbox.service.dataset.DatasetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class HarvestServiceImplTest {

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
  void harvest_notExceedingRecordLimitWithoutXslt_ExpectSuccess() throws HarvesterException {

    HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

    RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(1L);
    RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(2L);

    when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
        httpIterator);
    when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                                                        .thenReturn(recordEntity2);

    harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord());
    assertHarvestProcess(recordPublishService, true, 2, Step.HARVEST_ZIP, 2);

  }

  @Test
  void harvest_exceedingRecordLimitWithoutXslt_ExpectSuccess() throws HarvesterException {
    harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
        recordRepository);

    HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

    RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(1L);

    when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
        httpIterator);
    when(datasetService.isXsltPresent("datasetId")).thenReturn(false);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1);

    harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord());
    assertHarvestProcess(recordPublishService, true, 1, Step.HARVEST_ZIP, 1);

  }

  @Test
  void harvest_notExceedingRecordLimitWithXslt_ExpectSuccess() throws HarvesterException {
    HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

    RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(1L);
    RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(2L);

    when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
        httpIterator);
    when(datasetService.isXsltPresent("datasetId")).thenReturn(true);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1)
                                                        .thenReturn(recordEntity2);

    harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord());
    assertHarvestProcess(recordPublishService, false, 2, Step.HARVEST_ZIP, 2);

  }

  @Test
  void harvest_exceedingRecordLimitWithXslt_ExpectSuccess() throws HarvesterException {

    harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
        recordRepository);

    HttpRecordIterator httpIterator = new TestUtils.TestHttpRecordIterator(prepareMockListForHttpIterator());

    RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(1L);

    when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenReturn(
        httpIterator);
    when(datasetService.isXsltPresent("datasetId")).thenReturn(true);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1);

    harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord());
    assertHarvestProcess(recordPublishService, false, 1, Step.HARVEST_ZIP, 1);

  }

  @Test
  void harvest_expectFail() throws HarvesterException {
    when(httpHarvester.createTemporaryHttpHarvestIterator(any(InputStream.class), any(CompressedFileExtension.class))).thenThrow(
        HarvesterException.class);

    assertThrows(ServiceException.class,
        () -> harvestService.harvest(new ByteArrayInputStream(new byte[0]), "datasetId", createMockEncapsulatedRecord()));
  }

  @Test
  void harvestOaiPmh_notExceedingLimitWithoutXslt_expectSuccess() throws HarvesterException {
    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

    OaiRecord mockOaiRecord = mock(OaiRecord.class);
    RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(1L);
    RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId");
    recordEntity2.setId(2L);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
    when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
    when(datasetService.isXsltPresent(anyString())).thenReturn(false);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1).thenReturn(recordEntity2);

    harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData);
    assertHarvestProcess(recordPublishService, true,2, Step.HARVEST_OAI_PMH, 2);
  }

  @Test
  void harvestOaiPmh_exceedingLimitWithoutXslt_expectSuccess() throws HarvesterException {
    harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
        recordRepository);
    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

    OaiRecord mockOaiRecord = mock(OaiRecord.class);
    RecordEntity recordEntity = new RecordEntity("", "", "datasetId");
    recordEntity.setId(1L);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
    when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
    when(datasetService.isXsltPresent(anyString())).thenReturn(false);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

    harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData);
    assertHarvestProcess(recordPublishService, true,1, Step.HARVEST_OAI_PMH, 1);
  }

  @Test
  void harvestOaiPmh_notExceedingLimitWithXslt_expectSuccess() throws HarvesterException {
    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

    OaiRecord mockOaiRecord = mock(OaiRecord.class);
    RecordEntity recordEntity1 = new RecordEntity("", "", "datasetId");
    recordEntity1.setId(1L);
    RecordEntity recordEntity2 = new RecordEntity("", "", "datasetId");
    recordEntity2.setId(2L);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
    when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
    when(datasetService.isXsltPresent(anyString())).thenReturn(true);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity1).thenReturn(recordEntity2);

    harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData);
    assertHarvestProcess(recordPublishService, false,2, Step.HARVEST_OAI_PMH, 2);
  }

  @Test
  void harvestOaiPmh_exceedingLimitWithXslt_expectSuccess() throws HarvesterException {
    harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, recordPublishService, datasetService, 1,
        recordRepository);
    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");

    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestUtils.TestHeaderIterator(prepareListForOaiRecordIterator());

    OaiRecord mockOaiRecord = mock(OaiRecord.class);
    RecordEntity recordEntity = new RecordEntity("", "", "datasetId");
    recordEntity.setId(1L);

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(oaiRecordHeaderIterator);
    when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(mockOaiRecord);
    when(mockOaiRecord.getRecord()).thenReturn(new ByteArrayInputStream("record".getBytes(StandardCharsets.UTF_8)));
    when(datasetService.isXsltPresent(anyString())).thenReturn(true);
    when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

    harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData);
    assertHarvestProcess(recordPublishService, false, 1, Step.HARVEST_OAI_PMH, 1);
  }

  @Test
  void harvestOaiPmh_expectFail() throws HarvesterException {
    OaiHarvestData oaiHarvestData = new OaiHarvestData("url", "setspec", "metadaformat", "oaiIdentifier");
    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenThrow(HarvesterException.class);
    assertThrows(ServiceException.class,
        () -> harvestService.harvestOaiPmh("datasetId", createMockEncapsulatedRecord(), oaiHarvestData));
  }

  private void assertHarvestProcess(RecordPublishService recordPublishService, boolean harvestInEdmInternal, int times, Step step,
      int numberOfRecords) {
    if (harvestInEdmInternal) {
      verify(recordPublishService, times(times)).publishToHarvestQueue(captorRecordInfo.capture(), eq(step));
    } else {
      verify(recordPublishService, times(times)).publishToTransformationToEdmExternalQueue(captorRecordInfo.capture(), eq(step));
    }
    verify(datasetService).updateNumberOfTotalRecord(eq("datasetId"), eq(numberOfRecords));
    assertTrue(captorRecordInfo.getAllValues().stream().allMatch(x -> x.getRecord().getDatasetId().equals("datasetId")));
    assertTrue(captorRecordInfo.getAllValues().stream().allMatch(x -> x.getRecord().getContent() != null));
    assertEquals(Country.NETHERLANDS, captorRecordInfo.getValue().getRecord().getCountry());
    assertEquals(Language.NL, captorRecordInfo.getValue().getRecord().getLanguage());
    assertEquals("datasetName", captorRecordInfo.getValue().getRecord().getDatasetName());
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

  private Record.RecordBuilder createMockEncapsulatedRecord() {
    return Record.builder()
                 .datasetId("datasetId")
                 .country(Country.NETHERLANDS)
                 .language(Language.NL)
                 .datasetName("datasetName");
  }

  private List<OaiRecordHeader> prepareListForOaiRecordIterator() {
    OaiRecordHeader element1 = new OaiRecordHeader("oaiIdentifier1", false, Instant.now());
    OaiRecordHeader element2 = new OaiRecordHeader("oaiIdentifier2", false, Instant.now());
    List<OaiRecordHeader> iteratorList = new ArrayList<>();
    iteratorList.add(element1);
    iteratorList.add(element2);
    return iteratorList;
  }

}


