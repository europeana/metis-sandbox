package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
public class HarvestPublishServiceImplTest {

    @Mock
    private HarvestService harvestService;

    private final Executor taskExecutor = Runnable::run;

    private HarvestPublishServiceImpl asyncHarvestPublishService;

    @BeforeEach
    void setUp() {
        asyncHarvestPublishService = new HarvestPublishServiceImpl(harvestService, taskExecutor);
    }


    @Test
    void runZipHarvestAsync_expectSuccess() throws IOException, HarvesterException {
        Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
        MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                Files.newInputStream(dataSetPath));
        asyncHarvestPublishService.runHarvestZipAsync(datasetFile, "datasetName", "datasetId", Country.NETHERLANDS, Language.NL, null);

        verify(harvestService, times(1)).harvest(any(InputStream.class), eq("datasetId"), any(Record.RecordBuilder.class), null);

    }

    @Test
    void runZipHarvestAsync_expectFail() throws IOException {
        Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
        MockMultipartFile datasetFile = spy(new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                Files.newInputStream(dataSetPath)));

        when(datasetFile.getInputStream()).thenThrow(new IOException("error test"));

        assertThrows(ServiceException.class, () ->
                asyncHarvestPublishService.runHarvestZipAsync(datasetFile, "datasetName", "datasetId", Country.NETHERLANDS, Language.NL, null));

    }

    @Test
    void runHttpHarvestAsync_expectSuccess() throws HarvesterException {
        asyncHarvestPublishService.runHarvestHttpZipAsync("http://ftp.eanadev.org/uploads/Hauenstein-0.zip", "datasetName", "datasetId", Country.NETHERLANDS, Language.NL, null);
        verify(harvestService, times(1)).harvest(any(InputStream.class), eq("datasetId"), any(Record.RecordBuilder.class), null);

    }

    @Test
    void runHttpHarvestAsync_expectFail() {
        assertThrows(ServiceException.class, () ->
                asyncHarvestPublishService.runHarvestHttpZipAsync("http://myfake-test-url.com", "datasetName", "datasetId", Country.NETHERLANDS, Language.NL, null));

    }

    @Test
    void runHarvestOaiAsync_expectSuccess() {
        asyncHarvestPublishService.runHarvestOaiPmhAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL, null,
                new OaiHarvestData("url", "setspec", "metadataformat", "oaiIdentifier"));
        verify(harvestService, times(1)).harvestOaiPmh(eq("datasetId"), any(Record.RecordBuilder.class), any(OaiHarvestData.class), null);

    }

}
