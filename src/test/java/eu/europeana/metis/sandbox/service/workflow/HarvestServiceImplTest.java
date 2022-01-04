package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.ReportingIteration;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvest;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeaderIterator;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.HarvestContent;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.io.input.NullInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zeroturnaround.zip.ZipException;

@ExtendWith(SpringExtension.class)
public class HarvestServiceImplTest {

  private final TestUtils testUtils = new TestUtils();

  private static final HttpHarvester httpHarvester = spy(HarvesterFactory.createHttpHarvester());

  private static final OaiHarvester oaiHarvester = mock(OaiHarvester.class);

  private static final HarvestServiceImpl harvestService = new HarvestServiceImpl(httpHarvester,
      oaiHarvester);

  @Test
  void harvestServiceFromURL_noRecordLimit_ExpectSuccess() throws IOException {

    harvestService.setMaxRecords(1000);

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(dataSetPath));

    var expectedRecords = testUtils.getContentFromZipFile(Files.newInputStream(dataSetPath));
    Set<Integer> expectedRecordsLengths = new HashSet<>();
    expectedRecords.forEach(er -> expectedRecordsLengths.add(er.readAllBytes().length));

    HarvestContent harvestContent = harvestService.harvestZipUrl(dataSetPath.toUri().toString());
    Set<Integer> recordsLengths = new HashSet<>();
    harvestContent.getContent().forEach(r -> recordsLengths.add(r.readAllBytes().length));

    assertEquals(expectedRecordsLengths, recordsLengths);
    assertFalse(harvestContent.hasReachedRecordLimit());
    assertEquals(expectedRecords.size(), harvestContent.getContent().size());
  }

  @Test
  void harvestServiceFromURL_withRecordLimit_ExpectSuccess() throws IOException {

    harvestService.setMaxRecords(2);

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(dataSetPath));

    var expectedRecords = testUtils.getContentFromZipFile(Files.newInputStream(dataSetPath));
    Set<Integer> expectedRecordsLengths = new HashSet<>();
    expectedRecords.forEach(er -> expectedRecordsLengths.add(er.readAllBytes().length));

    HarvestContent harvestContent = harvestService.harvestZipUrl(dataSetPath.toUri().toString());
    Set<Integer> recordsLengths = new HashSet<>();
    harvestContent.getContent().forEach(r -> recordsLengths.add(r.readAllBytes().length));

    assertTrue(expectedRecordsLengths.containsAll(recordsLengths));
    assertTrue(harvestContent.hasReachedRecordLimit());
    assertEquals(2, harvestContent.getContent().size());
  }

  @Test
  void harvestServiceFromUploadedFile_noRecordLimit_ExpectSuccess() throws IOException {

    harvestService.setMaxRecords(1000);

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(dataSetPath));

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath));

    var expectedRecords = testUtils.getContentFromZipFile(Files.newInputStream(dataSetPath));
    Set<Integer> expectedRecordsLengths = new HashSet<>();
    expectedRecords.forEach(er -> expectedRecordsLengths.add(er.readAllBytes().length));

    var records = harvestService.harvestZipMultipartFile(datasetFile);
    Set<Integer> recordsLengths = new HashSet<>();
    records.getContent().forEach(r -> recordsLengths.add(r.readAllBytes().length));

    assertEquals(expectedRecordsLengths, recordsLengths);
    assertFalse(records.hasReachedRecordLimit());
    assertEquals(expectedRecords.size(), records.getContent().size());
  }

  @Test
  void harvestServiceFromUploadedFile_withRecordLimit_ExpectSuccess() throws IOException {

    harvestService.setMaxRecords(2);

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(dataSetPath));

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath));

    var expectedRecords = testUtils.getContentFromZipFile(Files.newInputStream(dataSetPath));
    Set<Integer> expectedRecordsLengths = new HashSet<>();
    expectedRecords.forEach(er -> expectedRecordsLengths.add(er.readAllBytes().length));

    var records = harvestService.harvestZipMultipartFile(datasetFile);
    Set<Integer> recordsLengths = new HashSet<>();
    records.getContent().forEach(r -> recordsLengths.add(r.readAllBytes().length));

    assertTrue(expectedRecordsLengths.containsAll(recordsLengths));
    assertTrue(records.hasReachedRecordLimit());
    assertEquals(2, records.getContent().size());
  }

  @Test
  void harvestServiceFromURL_NonExisting_ExpectFail() {

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "non-existing.zip");

    assertFalse(Files.exists(dataSetPath));

    assertThrows(ServiceException.class,
        () -> harvestService.harvestZipUrl(dataSetPath.toUri().toString()));
  }

  @Test
  void harvestServiceFromFile_CorruptFile_ExpectFail() throws IOException {

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "corrupt_file.zip");

    assertTrue(Files.exists(dataSetPath));

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath));

    assertThrows(ZipException.class, () -> harvestService.harvestZipMultipartFile(datasetFile));
  }

  @Test
  void harvestServiceFromFile_NonExistingFile_ExpectFail() throws IOException {

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        new NullInputStream());

    assertThrows(ZipException.class, () -> harvestService.harvestZipMultipartFile(datasetFile));

  }

  @Test
  void harvestServiceFromOai_noRecordLimit_ExpectSuccess() throws HarvesterException {

    harvestService.setMaxRecords(1000);

    OaiRecordHeader recordHeader = new OaiRecordHeader("someId", false, Instant.now());
    OaiRecord oaiRecord = new OaiRecord(recordHeader, () -> "record".getBytes(StandardCharsets.UTF_8));
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(List.of(recordHeader));

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
        oaiRecordHeaderIterator);

    when(oaiHarvester.harvestRecord(any(OaiRepository.class), eq("someId"))).thenReturn(oaiRecord);

    var harvestContent = harvestService.
        harvestOaiPmhEndpoint("someEndpointURL", "somePrefix", "someSetSpec");

    assertEquals(1, harvestContent.getContent().size());
    assertFalse(harvestContent.hasReachedRecordLimit());
    assertEquals("record", new String(harvestContent.getContent().get(0).readAllBytes(), StandardCharsets.UTF_8));

  }

  @Test
  void harvestServiceFromOai_withRecordLimit_ExpectSuccess() throws HarvesterException {

    harvestService.setMaxRecords(2);

    OaiRecordHeader recordHeader1 = new OaiRecordHeader("someId1", false, Instant.now());
    OaiRecordHeader recordHeader2 = new OaiRecordHeader("someId2", false, Instant.now());
    OaiRecordHeader recordHeader3 = new OaiRecordHeader("someId3", false, Instant.now());
    OaiRecord oaiRecord1 = new OaiRecord(recordHeader1, () -> "record".getBytes(StandardCharsets.UTF_8));
    OaiRecord oaiRecord2 = new OaiRecord(recordHeader1, () -> "record".getBytes(StandardCharsets.UTF_8));
    OaiRecord oaiRecord3 = new OaiRecord(recordHeader1, () -> "record".getBytes(StandardCharsets.UTF_8));
    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(List.of(recordHeader1, recordHeader2, recordHeader3));

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
        oaiRecordHeaderIterator);

    when(oaiHarvester.harvestRecord(any(OaiRepository.class), eq("someId1"))).thenReturn(oaiRecord1);
    when(oaiHarvester.harvestRecord(any(OaiRepository.class), eq("someId2"))).thenReturn(oaiRecord2);
    when(oaiHarvester.harvestRecord(any(OaiRepository.class), eq("someId3"))).thenReturn(oaiRecord3);

    HarvestContent harvestContent = harvestService
        .harvestOaiPmhEndpoint("someEndpointURL", "somePrefix", "someSetSpec");

    assertEquals(2, harvestContent.getContent().size());
    assertTrue(harvestContent.hasReachedRecordLimit());
    assertEquals("record", new String(harvestContent.getContent().get(0).readAllBytes(), StandardCharsets.UTF_8));

  }

  @Test
  void harvestServiceFromOai_ExpectServiceException() throws HarvesterException {

    OaiRecordHeaderIterator oaiRecordHeaderIterator = new TestHeaderIterator(
        Collections.emptyList());

    when(oaiHarvester.harvestRecordHeaders(any(OaiHarvest.class))).thenReturn(
        oaiRecordHeaderIterator);

    assertThrows(ServiceException.class, () -> harvestService
        .harvestOaiPmhEndpoint("someEndpointURL", "somePrefix", "someSetSpec"));

  }

  private static class TestHeaderIterator implements OaiRecordHeaderIterator {

    private final List<OaiRecordHeader> source;

    private TestHeaderIterator(
        List<OaiRecordHeader> source) {
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


