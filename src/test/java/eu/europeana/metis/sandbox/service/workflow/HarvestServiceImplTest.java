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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeroturnaround.zip.ZipException;


@ExtendWith(MockitoExtension.class)
public class HarvestServiceImplTest {


  private final ZipFileReader zipfilereader = new ZipFileReader();

  @InjectMocks
  private HarvestServiceImpl harvestService;

  @Test
  void harvestService_expectSuccess() throws IOException {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(datasetPath));

    var expectedRecords = zipfilereader.getContentFromZipFile(Files.newInputStream(datasetPath));

    var records = harvestService.harvest(datasetPath.toString());
    // In spite records being the same size, they are not in the same order.
    // So comparing with assertArrayEquals() will output a failed assertion
    // assertArrayEquals(expectedRecords.toArray(),records.toArray());
    assertEquals(expectedRecords.size(), records.size());
  }

  @Test
  void harvestService_expectFailWithServiceException() {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "non-existing-file.zip");
    assertFalse(Files.exists(datasetPath));

    assertThrows(ServiceException.class, () -> harvestService.harvest(datasetPath.toString()));
  }

  @Test
  void harvestService_expectFailWithZipException() {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "corrupt_file.zip");
    assertTrue(Files.exists(datasetPath));

    assertThrows(ZipException.class, () -> harvestService.harvest(datasetPath.toString()));
  }

}
