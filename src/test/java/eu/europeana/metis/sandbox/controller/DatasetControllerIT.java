package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.TestUtils;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@Disabled(
    "Disabled until fixed, the url initialization of the default xslt is not working if not an http!")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = SandboxApplication.class)
@AutoConfigureMockMvc
class DatasetControllerIT {

  private final TestUtils testUtils = new TestUtils();

  @Autowired private MockMvc mvc;

  @Test
  public void harvestDatasetWithFile_expectStatus_accepted() throws Exception {

    MockMultipartFile dataset =
        new MockMultipartFile(
            "dataset",
            "dataset.txt",
            "text/plain",
            testUtils.readFileToBytes("zip" + File.separator + "dataset-valid.zip"));

    mvc.perform(
            multipart("/dataset/{name}/harvestByFile", "my-data-set")
                .file(dataset)
                .param("country", ITALY.xmlValue())
                .param("language", IT.xmlValue()))
        .andExpect(status().isAccepted());
  }

  private static Stream<Arguments> provideZipTestFiles() {
    return Stream.of(
        Arguments.of("dataset-valid-with-corrupt-record.zip"),
        Arguments.of("dataset-with-corrupt-records.zip"));
  }

  @ParameterizedTest
  @MethodSource("provideZipTestFiles")
  public void harvestDatasetWithFile_withCorruptRecords_expectStatus_accepted(final String fileName)
      throws Exception {

    MockMultipartFile dataset =
        new MockMultipartFile(
            "dataset",
            "dataset.txt",
            "text/plain",
            testUtils.readFileToBytes("zip" + File.separator + fileName));

    mvc.perform(
            multipart("/dataset/{name}/harvestByFile", "my-data-set")
                .file(dataset)
                .param("country", ITALY.xmlValue())
                .param("language", IT.xmlValue()))
        .andExpect(status().isAccepted());
  }

  @Test
  public void harvestDatasetWithUrl_expectStatus_accepted() throws Exception {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(datasetPath));

    mvc.perform(
            post("/dataset/{name}/harvestByUrl", "my-data-set")
                .param("country", ITALY.xmlValue())
                .param("language", IT.xmlValue())
                .param("url", datasetPath.toUri().toString()))
        .andExpect(status().isAccepted());
  }

  // TODO: This sort of integration test should be addressed differently,
  //  with wiremock or pointing to a local URL repository
  public void harvestDatasetWithOAI_PMH_expectStatus_accepted() throws Exception {

    mvc.perform(
            post("/dataset/{name}/harvestOaiPmh", "my-data-set")
                .param("country", ITALY.xmlValue())
                .param("language", IT.xmlValue())
                .param("url", new URI("http://panic.image.ntua.gr:9000/efg/oai").toString())
                .param("setspec", "1073")
                .param("metadataformat", "rdf")
                .param("incremental", "false"))
        .andExpect(status().isAccepted());
  }

  @Test
  public void retrieveDataset_expectStatus_ok() throws Exception {
    mvc.perform(get("/dataset/{id}", "1")).andExpect(status().isOk());
  }
}
