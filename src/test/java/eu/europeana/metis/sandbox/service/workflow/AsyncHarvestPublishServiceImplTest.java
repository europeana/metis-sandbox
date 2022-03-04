package eu.europeana.metis.sandbox.service.workflow;

import eu.europeana.metis.harvesting.HarvesterException;
import eu.europeana.metis.sandbox.common.OaiHarvestData;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.domain.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;

@ExtendWith(MockitoExtension.class)
public class AsyncHarvestPublishServiceImplTest {

    @Mock
    private HarvestService harvestService;

    private final Executor taskExecutor = Runnable::run;

    private AsyncHarvestPublishServiceImpl asyncHarvestPublishService;

    @BeforeEach
    void setUp(){
        asyncHarvestPublishService = new AsyncHarvestPublishServiceImpl(harvestService, taskExecutor);
    }


    @Test
    void runZipHarvestAsync_expectSuccess() throws IOException, HarvesterException {
        Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
        MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                Files.newInputStream(dataSetPath));
        asyncHarvestPublishService.runZipHarvestAsync(datasetFile, "datasetName", "datasetId", Country.NETHERLANDS, Language.NL);

        verify(harvestService, times(1)).harvest(any(InputStream.class), eq("datasetId"), any(Record.RecordBuilder.class));

    }

    @Test
    void runZipHarvestAsync_expectFail() throws IOException {
        Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
        MockMultipartFile datasetFile = spy(new MockMultipartFile("dataset", "dataset.txt", "text/plain",
                Files.newInputStream(dataSetPath)));

        when(datasetFile.getInputStream()).thenThrow(new IOException("error test"));

        assertThrows(ServiceException.class, () ->
                asyncHarvestPublishService.runZipHarvestAsync(datasetFile, "datasetName", "datasetId", Country.NETHERLANDS, Language.NL));

    }

    @Test
    void runHttpHarvestAsync_expectSuccess() throws HarvesterException {
        asyncHarvestPublishService.runHttpHarvestAsync("http://ftp.eanadev.org/uploads/Hauenstein-0.zip", "datasetName", "datasetId", Country.NETHERLANDS, Language.NL);
        verify(harvestService, times(1)).harvest(any(InputStream.class), eq("datasetId"), any(Record.RecordBuilder.class));

    }

    @Test
    void runHttpHarvestAsync_expectFail()  {
        assertThrows(ServiceException.class, () ->
                asyncHarvestPublishService.runHttpHarvestAsync("http://fake-url.com", "datasetName", "datasetId", Country.NETHERLANDS, Language.NL));

    }

    @Test
    void runHarvestOaiAsync_expectSuccess() {
        asyncHarvestPublishService.runHarvestOaiAsync("datasetName", "datasetId", Country.NETHERLANDS, Language.NL,
                new OaiHarvestData("url", "setspec", "metadataformat", "oaiIdentifier"));
        verify(harvestService, times(1)).harvestOaiPmh( eq("datasetId"), any(Record.RecordBuilder.class), any(OaiHarvestData.class));

    }

}