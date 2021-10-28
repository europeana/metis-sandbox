package eu.europeana.metis.sandbox.service.workflow;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import eu.europeana.metis.sandbox.common.TestUtils;
import eu.europeana.metis.sandbox.common.exception.ServiceException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;


@ExtendWith(SpringExtension.class)
public class HarvestServiceImplTest {


  private final TestUtils testUtils = new TestUtils();
  @MockBean
  private HarvestService harvestService;

  @Test
  void harvestFromURL_expectSuccess() throws IOException {

    String url = "zip" + File.separator + "dataset-valid.zip";

    var expectedRecords =
        List.of(new ByteArrayInputStream(testUtils.readFileToBytes(url)));

    when(harvestService.harvest(url)).thenReturn(expectedRecords);

    var result = harvestService.harvest(url);

    assertArrayEquals(expectedRecords.toArray(), result.toArray());
  }

  @Test
  void harvestFromURL_expectFail()  {

    String url = "zip" + File.separator + "dataset-valid.zip";

    when(harvestService.harvest(url)).thenThrow(
        new ServiceException("Error harvesting records from " + url, new Exception()));

    assertThrows(ServiceException.class, () -> harvestService.harvest(url));
  }

  @Test
  void harvestFromFileInputStream_expectSuccess() throws IOException {

    File file = FileUtils.getFile("src", "test", "resources", "zip", "dataset-valid.zip");
    FileInputStream fis = new FileInputStream(file);
    MultipartFile mfile = new MockMultipartFile("dataset-valid", fis);

    var expectedRecords = List.of(new ByteArrayInputStream(mfile.getBytes()));

    when(harvestService.harvest(mfile)).thenReturn(expectedRecords);

    var result = harvestService.harvest(mfile);

    assertArrayEquals(expectedRecords.toArray(), result.toArray());
  }

  @Test
  void harvestFromFileInputStream_expectFail() throws IOException {

    File file = FileUtils.getFile("src", "test", "resources", "zip", "dataset-valid.zip");
    FileInputStream fis = new FileInputStream(file);
    MultipartFile mfile = new MockMultipartFile("dataset-valid", fis);

    when(harvestService.harvest(mfile)).thenThrow(
        new ServiceException("Error harvesting records from " + mfile.getName(), new Exception()));

    assertThrows(ServiceException.class, () -> harvestService.harvest(mfile));
  }

}
