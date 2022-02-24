package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.harvesting.HarvesterFactory;
import eu.europeana.metis.harvesting.http.HttpHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiHarvester;
import eu.europeana.metis.harvesting.oaipmh.OaiRecord;
import eu.europeana.metis.harvesting.oaipmh.OaiRecordHeader;
import eu.europeana.metis.harvesting.oaipmh.OaiRepository;
import eu.europeana.metis.sandbox.common.HarvestContent;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.ServiceException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import eu.europeana.metis.sandbox.entity.RecordEntity;
import eu.europeana.metis.sandbox.repository.RecordRepository;
import org.apache.commons.io.input.NullInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import org.zeroturnaround.zip.ZipException;

@ExtendWith(SpringExtension.class)
public class HarvestServiceImplTest {

    private final TestUtils testUtils = new TestUtils();

    private static final HttpHarvester httpHarvester = spy(HarvesterFactory.createHttpHarvester());

    private static final OaiHarvester oaiHarvester = mock(OaiHarvester.class);

    private HarvestService harvestService;

    @Mock
    private RecordRepository recordRepository;

    @Test
    void harvestServiceFromURL_notExceedingRecordLimit_ExpectSuccess() throws IOException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);

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
    void harvestServiceFromURL_exceedingRecordLimit_ExpectSuccess() throws IOException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 2, recordRepository);

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
    void harvestServiceFromUploadedFile_notExceedingRecordLimit_ExpectSuccess() throws IOException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);

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
    void harvestServiceFromUploadedFile_exceedingRecordLimit_ExpectSuccess() throws IOException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 2, recordRepository);

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

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);

        Path dataSetPath = Paths.get("src", "test", "resources", "zip", "non-existing.zip");

        assertFalse(Files.exists(dataSetPath));

        assertThrows(ServiceException.class,
                () -> harvestService.harvestZipUrl(dataSetPath.toUri().toString()));
    }

    @Test
    void harvestServiceFromFile_CorruptFile_ExpectFail() throws IOException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);

        Path dataSetPath = Paths.get("src", "test", "resources", "zip", "corrupt_file.zip");

        assertTrue(Files.exists(dataSetPath));

        MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                Files.newInputStream(dataSetPath));

        assertThrows(ZipException.class, () -> harvestService.harvestZipMultipartFile(datasetFile));
    }

    @Test
    void harvestServiceFromFile_NonExistingFile_ExpectFail() throws IOException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);

        MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                new NullInputStream());

        assertThrows(ZipException.class, () -> harvestService.harvestZipMultipartFile(datasetFile));

    }

    @Test
    void harvestOaiRecordHeader_ExpectSuccess() throws HarvesterException {

        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);
        RecordEntity recordEntity = new RecordEntity(null, null, "1");
        OaiHarvestData oaiHarvestData = new OaiHarvestData("someEndpointURL", "someSetSpec", "somePrefix");
        Record record = Record.builder()
                .datasetId("1")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .build();

        OaiRecordHeader recordHeader = new OaiRecordHeader("someId", false, Instant.now());
        OaiRecord oaiRecord = new OaiRecord(recordHeader,
                () -> "record".getBytes(StandardCharsets.UTF_8));

        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenReturn(oaiRecord);
        when(recordRepository.save(any(RecordEntity.class))).thenReturn(recordEntity);

        var harvestContent = harvestService.harvestOaiRecordHeader(oaiHarvestData, record, recordHeader);

        assertEquals("1", harvestContent.getRecord().getDatasetId());
        assertEquals(Country.NETHERLANDS, harvestContent.getRecord().getCountry());
        assertEquals(Language.NL, harvestContent.getRecord().getLanguage());
        assertTrue(CollectionUtils.isEmpty(harvestContent.getErrors()));
        assertEquals("record", new String(harvestContent.getRecord().getContent(), StandardCharsets.UTF_8));

    }


    @Test
    void harvestOaiRecordHeader_ExpectFail() throws HarvesterException {
        harvestService = new HarvestServiceImpl(httpHarvester, oaiHarvester, 1000, recordRepository);
        OaiHarvestData oaiHarvestData = new OaiHarvestData("someEndpointURL", "someSetSpec", "somePrefix");
        Record record = Record.builder()
                .datasetId("1")
                .country(Country.NETHERLANDS)
                .language(Language.NL)
                .build();

        OaiRecordHeader recordHeader = new OaiRecordHeader("someId", false, Instant.now());

        when(oaiHarvester.harvestRecord(any(OaiRepository.class), anyString())).thenThrow(new HarvesterException("error test"));

        var harvestContent = harvestService.harvestOaiRecordHeader(oaiHarvestData, record, recordHeader);

        assertEquals(record, harvestContent.getRecord());
        assertFalse(CollectionUtils.isEmpty(harvestContent.getErrors()));
        assertEquals("Error harvesting OAI-PMH Record Header:someId", harvestContent.getErrors().get(0).getMessage());

    }
}


