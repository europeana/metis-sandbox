package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static org.junit.jupiter.api.Assertions.*;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.TestUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
class DatasetControllerIT {

  private final TestUtils testUtils = new TestUtils();
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

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
    assertTrue(response.getBody().contains("\"dataset-id\""));
  }

  //TODO: Create harvestDatasetWithFile scenario with xslt file included

  @Test
  public void harvestDatasetWithUrl_expectStatus_accepted() {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid.zip");
    assertTrue(Files.exists(datasetPath));

    HttpHeaders requestHeaders = new HttpHeaders();
    //As a request it is possible to upload a xsltFile, even if we don't want to include it,
    //hence we set content ty as multipart_form_data
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, String> body
            = new LinkedMultiValueMap<>();
    body.add("url", datasetPath.toUri().toString());
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    ResponseEntity<String> response =
            testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByUrl",
                    new HttpEntity<>(body, requestHeaders), String.class, "testDataset");

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"dataset-id\""));
  }

  //TODO: Create harvestDatasetWithFile scenario with xslt file included

//
  // TODO: This sort of integration test should be addressed differently,
  //  with wiremock or pointing to a local URL repository
//  public void harvestDatasetWithOAI_PMH_expectStatus_accepted() throws Exception {
//
//    mvc.perform(post("/dataset/{name}/harvestOaiPmh", "my-data-set")
//            .param("country", ITALY.xmlValue())
//            .param("language", IT.xmlValue())
//            .param("url", new URI("http://panic.image.ntua.gr:9000/efg/oai").toString())
//            .param("setspec", "1073")
//            .param("metadataformat", "rdf")
//            .param("incremental", "false"))
//        .andExpect(status().isAccepted());
//  }
//
  @Test
  public void retrieveDataset_expectStatus_ok() {
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


//TODO use dataset id retrieved from call
    ResponseEntity<String> getDatasetResponse =
            testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}", String.class, "1");

    assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
    assertNotNull(getDatasetResponse.getBody());
    assertTrue(getDatasetResponse.getBody().contains("\"status\":"));


  }

}
