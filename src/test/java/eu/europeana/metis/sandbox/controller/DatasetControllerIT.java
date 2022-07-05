package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

import com.jayway.awaitility.Awaitility;
import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.TestUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.scheduler.XsltUrlUpdateScheduler;
import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
class DatasetControllerIT {

  private final TestUtils testUtils = new TestUtils();
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  @Autowired
  private XsltUrlUpdateScheduler xsltUrlUpdateScheduler;

  @Value("${local.server.port}")
  private int port;

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    PostgresContainerInitializerIT.dynamicProperties(registry);
    RabbitMQContainerInitializerIT.properties(registry);
  }

  @BeforeEach
  void cleanUpPostgres(){
    PostgresContainerInitializerIT.runScripts(List.of("database/schema_drop.sql", "database/schema.sql"));
    xsltUrlUpdateScheduler.updateDefaultXsltUrl();
    System.out.println("testing");
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
    FileSystemResource xsltFile = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                    File.separator + "xslt-file.xslt");


    ResponseEntity<String> response = makeHarvestingByFile(dataset, xsltFile);
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
    FileSystemResource xsltFile = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                    File.separator + "xslt-file.xslt");

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body
            = new LinkedMultiValueMap<>();
    body.add("url", datasetPath.toUri().toString());
    body.add("xsltFile", xsltFile);
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
                    File.separator + "dataset-valid.zip");


    ResponseEntity<String> response = makeHarvestingByFile(dataset, null);;
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());


    //TODO wait for it to be completed?
    ResponseEntity<String> getDatasetResponse =
            testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}", String.class, "1");

    assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
    assertNotNull(getDatasetResponse.getBody());
    assertTrue(getDatasetResponse.getBody().contains("\"status\":"));

  }

  @Test
  void computeRecordTierCalculation_expectedSuccess() throws InterruptedException {
    FileSystemResource dataset = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                    File.separator + "dataset-valid.zip");
    ResponseEntity<String> responseHarvestingDataset = makeHarvestingByFile(dataset, null);
    assertEquals(HttpStatus.ACCEPTED, responseHarvestingDataset.getStatusCode());
    assertNotNull(responseHarvestingDataset.getBody());

//    Awaitility.await().atMost(10, SECONDS).until(() -> testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record/compute-tier-calculation?recordId={recordId}",
//            String.class, "1", "1/URN_NBN_SI_doc_35SZSOCF").getStatusCode() != HttpStatus.NOT_FOUND);
    Thread.sleep(10000);
    ResponseEntity<String> response =
            testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record/compute-tier-calculation?recordId={recordId}",
                    String.class, "1", "1/URN_NBN_SI_doc_35SZSOCF");


    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

  }

  @Test
  void getRecord_expectedSuccess(){
    //get {id}/record
  }

  @Test
  void getAllCountries_expectSuccess(){
    ResponseEntity<String> response =
            testRestTemplate.getForEntity(getBaseUrl() + "/dataset/countries", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    List<Country> countries = Country.getCountryListSortedByName();
    countries.forEach(country -> assertTrue(response.getBody().contains(country.xmlValue())));
  }

  @Test
  void getAllLanguages_expectSuccess(){
    ResponseEntity<String> response =
            testRestTemplate.getForEntity(getBaseUrl() + "/dataset/languages", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    List<Language> languages = Language.getLanguageListSortedByName();
    languages.forEach(language -> assertTrue(response.getBody().contains(language.xmlValue())));
  }

  private ResponseEntity<String> makeHarvestingByFile(FileSystemResource dataset, FileSystemResource xsltFile){

    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, Object> body
            = new LinkedMultiValueMap<>();
    body.add("dataset", dataset);
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    if(xsltFile != null){
      body.add("xsltFile", xsltFile);
    }

    return testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByFile",
            new HttpEntity<>(body, requestHeaders), String.class, "testDataset");
  }

}
