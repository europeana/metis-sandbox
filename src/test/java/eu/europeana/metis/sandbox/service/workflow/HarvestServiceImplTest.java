package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.exception.ServiceException;
import eu.europeana.metis.utils.ZipFileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.input.NullInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.zeroturnaround.zip.ZipException;


@ExtendWith(MockitoExtension.class)
public class HarvestServiceImplTest {

  private static final ZipFileReader zipfilereader = new ZipFileReader();

  private static final HarvestServiceImpl harvestService = new HarvestServiceImpl();

  @Test
  void harvestServiceFromURL_ExpectSuccess() throws IOException {

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(dataSetPath));

    var expectedRecords = zipfilereader.getContentFromZipFile(Files.newInputStream(dataSetPath));

    var records = harvestService.harvest(dataSetPath.toUri().toString());

    assertEquals(expectedRecords.size(), records.size());
  }

  @Test
  void harvestServiceFromUploadedFile_ExpectSuccess() throws IOException {

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");

    assertTrue(Files.exists(dataSetPath));

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath));

    var expectedRecords = zipfilereader.getContentFromZipFile(Files.newInputStream(dataSetPath));

    var records = harvestService.harvest(datasetFile);

    assertEquals(expectedRecords.size(), records.size());
  }

  @Test
  void harvestServiceFromURL_NonExisting_ExpectFail() {

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "non-existing.zip");

    assertFalse(Files.exists(dataSetPath));

    assertThrows(ServiceException.class,
        () -> harvestService.harvest(dataSetPath.toUri().toString()));
  }

  @Test
  void harvestServiceFromFile_CorruptFile_ExpectFail() throws IOException {

    Path dataSetPath = Paths.get("src", "test", "resources", "zip", "corrupt_file.zip");

    assertTrue(Files.exists(dataSetPath));

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        Files.newInputStream(dataSetPath));

    assertThrows(ZipException.class, () -> harvestService.harvest(datasetFile));
  }

  @Test
  void harvestServiceFromFile_NonExistingFile_ExpectFail() throws IOException {

    MockMultipartFile datasetFile = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        new NullInputStream());

    assertThrows(ZipException.class, () -> harvestService.harvest(datasetFile));
  }

}
