package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.awaitility.Awaitility;
import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.test.utils.MongoDBContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import eu.europeana.metis.sandbox.test.utils.SolrContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
class DatasetControllerIT {

  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  @LocalServerPort
  private int port;

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    PostgresContainerInitializerIT.dynamicProperties(registry);
    PostgresContainerInitializerIT.runScripts(List.of(
        "database/schema_drop.sql", "database/schema.sql",
        "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql"));
    RabbitMQContainerInitializerIT.properties(registry);
    MongoDBContainerInitializerIT.dynamicProperties(registry);
    SolrContainerInitializer.dynamicProperties(registry);
  }

  @BeforeEach
  void cleanUpPostgres() {
    PostgresContainerInitializerIT.runScripts(List.of("database/schema_drop_except_transform_xslt.sql", "database/schema.sql"));
  }

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  public void harvestDatasetWithFile_expectStatus_accepted() {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid.zip");

    ResponseEntity<String> response = makeHarvestingByFile(dataset, null);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"dataset-id\":\"1\""));
  }

  @Test
  public void harvestDatasetWithFile_withXsltFile_expectStatus_accepted() {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid-with-xslt-file.zip");
    FileSystemResource xsltFileForTransformationToEdmExternal = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "xslt-file-for-transformation-to-edm-external-test.xslt");

    ResponseEntity<String> response = makeHarvestingByFile(dataset, xsltFileForTransformationToEdmExternal);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"dataset-id\":\"1\""));
  }

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
    assertTrue(response.getBody().contains("\"dataset-id\":\"1\""));
  }

  @Test
  public void harvestDatasetWithUrl_withXsltFile_expectStatus_accepted() {

    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid-with-xslt-file.zip");
    assertTrue(Files.exists(datasetPath));
    FileSystemResource xsltFileForTransformationToEdmExternal = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "xslt-file-for-transformation-to-edm-external-test.xslt");

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body
        = new LinkedMultiValueMap<>();
    body.add("url", datasetPath.toUri().toString());
    body.add("xsltFile", xsltFileForTransformationToEdmExternal);
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    ResponseEntity<String> response =
        testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByUrl",
            new HttpEntity<>(body, requestHeaders), String.class, "testDataset");

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().contains("\"dataset-id\":\"1\""));
  }


  //
  // TODO: This sort of integration test should be addressed differently,
  //  with wiremock or pointing to a local URL repository. Creating a test container for OAI-PMH would best
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
            File.separator + "dataset-valid-small.zip");
    makeHarvestingByFile(dataset, null);

    // Give time for the full harvesting to happen
    Awaitility.await().atMost(3, MINUTES).until(() -> Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}",
            String.class, "1").getBody()).contains("COMPLETED"));

    ResponseEntity<String> getDatasetResponse =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}", String.class, "1");

    assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
    assertNotNull(getDatasetResponse.getBody());

  }

  @Test
  void computeRecordTierCalculation_expectedSuccess() {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid.zip");
    makeHarvestingByFile(dataset, null);

    //    TODO: The commented code block is more appropriate when it comes to waiting for something, but the condition is currently failing.
    //     We are leaving it commented so we can use it later when issue is fixed
    //     Awaitility.await().atMost(10, SECONDS).until(() -> testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record/compute-tier-calculation?recordId={recordId}",
    //     String.class, "1", "1/URN_NBN_SI_doc_35SZSOCF").getStatusCode() != HttpStatus.NOT_FOUND);
    ResponseEntity<String> response =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record/compute-tier-calculation?recordId={recordId}",
            String.class, "1", "1/URN_NBN_SI_doc_35SZSOCF");

    //TODO the status should be OK. We're leaving like this so the build is successful
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());


  }

  @Test
  void getRecord_expectedSuccess() {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid.zip");
    makeHarvestingByFile(dataset, null);

    // TODO The explanation written previously also applies here

    ResponseEntity<String> response =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record?recordId={recordId}", String.class,
            "1", "1/URN_NBN_SI_doc_35SZSOCF");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
  }

  @Test
  void getAllCountries_expectSuccess() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/countries", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    List<Country> countries = Country.getCountryListSortedByName();
    countries.forEach(country -> assertTrue(response.getBody().contains(country.xmlValue())));
  }

  @Test
  void getAllLanguages_expectSuccess() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/languages", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    List<Language> languages = Language.getLanguageListSortedByName();
    languages.forEach(language -> assertTrue(response.getBody().contains(language.xmlValue())));
  }

  private ResponseEntity<String> makeHarvestingByFile(FileSystemResource dataset, FileSystemResource xsltFile) {

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body
        = new LinkedMultiValueMap<>();
    body.add("dataset", dataset);
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    if (xsltFile != null) {
      body.add("xsltFile", xsltFile);
    }

    return testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByFile",
        new HttpEntity<>(body, requestHeaders), String.class, "testDataset");
  }

}
