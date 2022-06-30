package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.junit.jupiter.api.Assertions.*;
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
import java.util.List;
import java.util.stream.Stream;

import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
@AutoConfigureMockMvc
class DatasetControllerIT {

  private final TestUtils testUtils = new TestUtils();
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  @Autowired
  private MockMvc mvc;

  @Value("${local.server.port}")
  private int port;

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    PostgresContainerInitializerIT.dynamicProperties(registry);
    PostgresContainerInitializerIT.runScripts(List.of("database/schema_drop.sql", "database/schema.sql"));
    RabbitMQContainerInitializerIT.properties(registry);
  }

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  public void harvestDatasetWithFile_expectStatus_accepted() {
    FileSystemResource dataset = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                    File.separator + "dataset-valid.zip");


    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body
            = new LinkedMultiValueMap<>();
    body.add("dataset", dataset);
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());


    ResponseEntity<String> response =
            testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByFile",
                    new HttpEntity<>(body, requestHeaders), String.class, "testDataset");
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"dataset-id\":\"1\""));

  }

  @Test
  public void harvestDatasetWithUrl_expectStatus_accepted() throws Exception {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(datasetPath));

    mvc.perform(post("/dataset/{name}/harvestByUrl", "my-data-set")
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue())
            .param("url", datasetPath.toUri().toString()))
        .andExpect(status().isAccepted());
  }

  // TODO: This sort of integration test should be addressed differently,
  //  with wiremock or pointing to a local URL repository
  public void harvestDatasetWithOAI_PMH_expectStatus_accepted() throws Exception {

    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
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
    mvc.perform(get("/dataset/{id}", "1"))
        .andExpect(status().isOk());
  }

  private static Stream<Arguments> provideZipTestFiles() {
    return Stream.of(
        Arguments.of("dataset-one", "dataset-valid-with-corrupt-record.zip"),
        Arguments.of("dataset-two", "dataset-with-corrupt-records.zip"));
  }

  @ParameterizedTest
  @MethodSource("provideZipTestFiles")
  public void harvestDatasetWithFile_withCorruptRecords_expectStatus_accepted(final String datasetName, final String fileName)
      throws Exception {
    MockMultipartFile dataset = new MockMultipartFile("dataset", "dataset.txt", "text/plain",
        testUtils.readFileToBytes("zip" + File.separator + fileName));

    mvc.perform(multipart("/dataset/{name}/harvestByFile", datasetName)
            .file(dataset)
            .param("country", ITALY.xmlValue())
            .param("language", IT.xmlValue()))
        .andExpect(status().isAccepted());
  }
}
