package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import eu.europeana.metis.sandbox.domain.DatasetMetadata;
import eu.europeana.metis.sandbox.domain.Record;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import eu.europeana.metis.sandbox.service.dataset.HarvestingParameterService;
import eu.europeana.metis.utils.CompressedFileExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class HarvestPublishServiceImplTest {

  @Mock
  private HarvestService harvestService;

  @Mock
  private HarvestingParameterService harvestingParameterService;

  private final Executor taskExecutor = Runnable::run;

  private HarvestPublishServiceImpl asyncHarvestPublishService;

  @BeforeEach
  void setUp() {
    asyncHarvestPublishService = new HarvestPublishServiceImpl(harvestService, taskExecutor, harvestingParameterService);
  }


  @Test
  void runZipHarvestAsync_expectSuccess() throws IOException, HarvesterException {
    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath));
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .withDatasetId("datasetId")
                                                     .withDatasetName("datasetName")
                                                     .withCountry(Country.NETHERLANDS)
                                                     .withLanguage(Language.NL)
                                                     .withStepSize(5)
                                                     .build();
    asyncHarvestPublishService.runHarvestProvidedFileAsync(datasetFile, datasetMetadata, CompressedFileExtension.ZIP);

    verify(harvestService, times(1)).harvestFromCompressedArchive(any(InputStream.class), eq("datasetId"), any(Record.RecordBuilder.class), eq(5),
            eq(CompressedFileExtension.ZIP));

  }

  @Test
  void runZipHarvestAsync_expectFail() throws IOException {
    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    MockMultipartFile datasetFile = spy(new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath)));
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .withDatasetId("datasetId")
                                                     .withDatasetName("datasetName")
                                                     .withCountry(Country.NETHERLANDS)
                                                     .withLanguage(Language.NL)
                                                     .withStepSize(5)
                                                     .build();

    when(datasetFile.getInputStream()).thenThrow(new IOException("error test"));

    assertThrows(ServiceException.class, () ->
        asyncHarvestPublishService.runHarvestProvidedFileAsync(datasetFile, datasetMetadata, CompressedFileExtension.ZIP));

  }

  @Test
  void runHttpHarvestAsync_expectSuccess() throws HarvesterException {
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .withDatasetId("datasetId")
                                                     .withDatasetName("datasetName")
                                                     .withCountry(Country.NETHERLANDS)
                                                     .withLanguage(Language.NL)
                                                     .withStepSize(5)
                                                     .build();

    CompletableFuture<Void> future = asyncHarvestPublishService.runHarvestHttpFileAsync(
        "http://ftp.eanadev.org/uploads/Hauenstein-0.zip", datasetMetadata, CompressedFileExtension.ZIP);

    assertTrue(!future.isCompletedExceptionally() || future.isCancelled());
    verify(harvestService, times(1)).harvestFromCompressedArchive(any(InputStream.class), eq("datasetId"), any(Record.RecordBuilder.class), eq(5),
            eq(CompressedFileExtension.ZIP));
  }

  @Test
  void runHttpHarvestAsync_expectFail() {
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .withDatasetId("datasetId")
                                                     .withDatasetName("datasetName")
                                                     .withCountry(Country.NETHERLANDS)
                                                     .withLanguage(Language.NL)
                                                     .withStepSize(5)
                                                     .build();
    assertThrows(ServiceException.class, ()-> asyncHarvestPublishService.runHarvestHttpFileAsync("http://myfake-test-url.com",
        datasetMetadata,null));
  }

  @Test
  void runHarvestOaiAsync_expectSuccess() {
    DatasetMetadata datasetMetadata = DatasetMetadata.builder()
                                                     .withDatasetId("datasetId")
                                                     .withDatasetName("datasetName")
                                                     .withCountry(Country.NETHERLANDS)
                                                     .withLanguage(Language.NL)
                                                     .withStepSize(5)
                                                     .build();
    asyncHarvestPublishService.runHarvestOaiPmhAsync(datasetMetadata,
        new OaiHarvestData("url", "setspec", "metadataformat", "oaiIdentifier"));
    verify(harvestService, times(1)).harvestFromOaiPmh(eq("datasetId"), any(Record.RecordBuilder.class), any(OaiHarvestData.class),
        eq(5));
  }
}
