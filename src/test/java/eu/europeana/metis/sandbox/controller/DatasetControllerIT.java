package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.awaitility.Awaitility;
import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.test.utils.TestContainer;
import eu.europeana.metis.sandbox.test.utils.TestContainerFactoryIT;
import eu.europeana.metis.sandbox.test.utils.TestContainerType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
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
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
class DatasetControllerIT {

  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  @LocalServerPort
  private int port;

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    TestContainer postgresql = TestContainerFactoryIT.getContainer(TestContainerType.POSTGRES);
    postgresql.dynamicProperties(registry);
    postgresql.runScripts(List.of("database/schema_drop_except_transform_xslt.sql", "database/schema.sql"));
    postgresql.runScripts(List.of("database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql"));
    postgresql.runScripts(List.of("database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql"));
    TestContainer rabbitMQ = TestContainerFactoryIT.getContainer(TestContainerType.RABBITMQ);
    rabbitMQ.dynamicProperties(registry);
    TestContainer mongoDBContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.MONGO);
    mongoDBContainerIT.dynamicProperties(registry);
    TestContainer solrContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.SOLR);
    solrContainerIT.dynamicProperties(registry);
    TestContainer s3ContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.S3);
    s3ContainerIT.dynamicProperties(registry);
  }

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  @Test
  public void harvestDatasetWithFile_expectStatus_accepted() {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid-small.zip");

    ResponseEntity<String> response = makeHarvestingByFile(dataset, null);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(response.getBody());
    assertTrue(expectedDatasetId > 0);
  }

  @Test
  public void harvestDatasetWithFile_withXsltFile_expectStatus_accepted() {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid-with-xslt-file-small.zip");
    FileSystemResource xsltFileForTransformationToEdmExternal = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "xslt-file-for-transformation-to-edm-external-test.xslt");

    ResponseEntity<String> response = makeHarvestingByFile(dataset, xsltFileForTransformationToEdmExternal);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(response.getBody());
    assertTrue(expectedDatasetId > 0);
  }

  @Test
  public void harvestDatasetWithUrl_expectStatus_accepted() {
    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid-small.zip");
    assertTrue(Files.exists(datasetPath));

    HttpHeaders requestHeaders = new HttpHeaders();
    //As a request it is possible to upload a xsltFile, even if we don't want to include it,
    //hence we set content ty as multipart_form_data
    requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("url", datasetPath.toUri().toString());
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    ResponseEntity<String> response =
        testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByUrl",
            new HttpEntity<>(body, requestHeaders), String.class, "testDataset");

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(response.getBody());
    assertTrue(expectedDatasetId > 0);
  }

  @Test
  public void harvestDatasetWithUrl_withXsltFile_expectStatus_accepted() {
    Path datasetPath = Paths.get("src", "test", "resources", "zip", "dataset-valid-with-xslt-file-small.zip");
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
    assertTrue(response.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(response.getBody());
    assertTrue(expectedDatasetId > 0);

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
  public void retrieveDatasetProgress_expectStatus_ok() throws IOException {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid-small.zip");
    FileSystemResource datasetResponseBody = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" +
            File.separator + "zip" + File.separator + "responsefiles" + File.separator +
                "response_body_dataset_valid_small.txt");
    String datasetResponseBodyContent = new String(datasetResponseBody.getInputStream().readAllBytes());

    ResponseEntity<String> responseDataset = makeHarvestingByFile(dataset, null);
    assertTrue(responseDataset.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(responseDataset.getBody());
    assertTrue(expectedDatasetId > 0);

    // Give time for the full harvesting to happen
    Awaitility.await().atMost(10, MINUTES)
              .until(() -> Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress",
                  String.class, expectedDatasetId).getBody()).contains("COMPLETED"));

    ResponseEntity<String> getDatasetResponse =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress", String.class, expectedDatasetId);

    assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
    assertNotNull(getDatasetResponse.getBody());
    JSONAssert.assertEquals(StringUtils.deleteWhitespace(datasetResponseBodyContent),
        StringUtils.deleteWhitespace(getDatasetResponse.getBody()), true);


  }

  @Test
  void retrieveDatasetInfo_expectStatus_ok() throws IOException {
    FileSystemResource dataset = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                    File.separator + "dataset-valid-small.zip");
    FileSystemResource datasetResponseBody = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" +
                    File.separator + "zip" + File.separator + "responsefiles" + File.separator +
                    "get_dataset_info_response_body.txt");
    String datasetResponseBodyContent = new String(datasetResponseBody.getInputStream().readAllBytes());

    ResponseEntity<String> responseDataset = makeHarvestingByFile(dataset, null);
    assertTrue(responseDataset.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(responseDataset.getBody());
    assertTrue(expectedDatasetId > 0);

    ResponseEntity<String> getDatasetResponse =
            testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/info", String.class, expectedDatasetId);

    assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
    assertNotNull(getDatasetResponse.getBody());
    assertTrue(getDatasetResponse.getBody().contains("\"creation-date\""));
    JSONAssert.assertEquals(StringUtils.deleteWhitespace(datasetResponseBodyContent),
            StringUtils.deleteWhitespace(removeCreationDate(getDatasetResponse.getBody())), true);


  }

  @Test
  void computeRecordTierCalculation_expectedSuccess() throws IOException {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid-small.zip");
    FileSystemResource tierCalculationResponse = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" +
            File.separator + "zip" + File.separator + "responsefiles" + File.separator +
                "tier_calculation_response_body.txt");
    String tierCalculationResponseContent = new String(tierCalculationResponse.getInputStream().readAllBytes());
    ResponseEntity<String> responseDataset = makeHarvestingByFile(dataset, null);
    assertTrue(responseDataset.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(responseDataset.getBody());
    assertTrue(expectedDatasetId > 0);

    Awaitility.await().atMost(10, MINUTES).until(() -> testRestTemplate.getForEntity(getBaseUrl() +
            "/dataset/{id}/record/compute-tier-calculation?recordId={recordId}",
        String.class, expectedDatasetId, "/" + expectedDatasetId + "/URN_NBN_SI_doc_B1HM2TA6").getStatusCode()
        != HttpStatus.NOT_FOUND);
    ResponseEntity<String> response =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record/compute-tier-calculation?recordId={recordId}",
            String.class, expectedDatasetId, "/" + expectedDatasetId + "/URN_NBN_SI_doc_B1HM2TA6");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    JSONAssert.assertEquals(expectedContentTierJson(StringUtils.deleteWhitespace(tierCalculationResponseContent),
            "/" + expectedDatasetId + "/URN_NBN_SI_doc_B1HM2TA6"),
        StringUtils.deleteWhitespace(response.getBody()), true);
  }

  @Test
  void getRecord_expectedSuccess() throws IOException {
    FileSystemResource dataset = new FileSystemResource(
        "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
            File.separator + "dataset-valid-small.zip");
    FileSystemResource getRecordBody = new FileSystemResource("src" + File.separator + "test" + File.separator + "resources" +
        File.separator + "zip" + File.separator + "responsefiles" + File.separator + "get_record_response_body.txt");
    String getRecordBodyContent = new String(getRecordBody.getInputStream().readAllBytes());
    ResponseEntity<String> responseDataset = makeHarvestingByFile(dataset, null);
    assertTrue(responseDataset.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(responseDataset.getBody());
    assertTrue(expectedDatasetId > 0);

    Awaitility.await().atMost(10, MINUTES)
              .until(() -> testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record?recordId={recordId}",
                  String.class, expectedDatasetId, "/" + expectedDatasetId + "/URN_NBN_SI_doc_B1HM2TA6").getStatusCode()
                  != HttpStatus.NOT_FOUND);

    ResponseEntity<String> response =
        testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/record?recordId={recordId}", String.class,
            expectedDatasetId, "/" + expectedDatasetId + "/URN_NBN_SI_doc_B1HM2TA6");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(StringUtils.deleteWhitespace(getRecordBodyContent), StringUtils.deleteWhitespace(response.getBody()));
  }

  @Test
  void getRecordsTier_expectSuccess() throws IOException {
    FileSystemResource dataset = new FileSystemResource(
            "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                    File.separator + "dataset-valid-small.zip");
    FileSystemResource getRecordBody = new FileSystemResource("src" + File.separator + "test" + File.separator + "resources" +
            File.separator + "zip" + File.separator + "responsefiles" + File.separator + "get_records_tiers_response_body.txt");
    String getRecordBodyContent = new String(getRecordBody.getInputStream().readAllBytes());
    ResponseEntity<String> responseDataset = makeHarvestingByFile(dataset, null);
    assertTrue(responseDataset.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
    final int expectedDatasetId = extractDatasetId(responseDataset.getBody());
    assertTrue(expectedDatasetId > 0);

    String recordId1 = "/" + expectedDatasetId + "/URN_NBN_SI_doc_B1HM2TA6";
    String recordId2 = "/" + expectedDatasetId + "/URN_NBN_SI_doc_35SZSOCF";

    Awaitility.await().atMost(10, MINUTES)
            .until(() -> {
              String body = Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/records-tiers",
                      String.class, expectedDatasetId).getBody());

              return body.contains(recordId1) && body.contains(recordId2);
            });

    ResponseEntity<String> response = testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/records-tiers",
            String.class, expectedDatasetId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    JSONAssert.assertEquals(StringUtils.deleteWhitespace(removeRecordIdFields(getRecordBodyContent)),
            StringUtils.deleteWhitespace(removeRecordIdFields(response.getBody())), true);

  }

  @Test
  void getRecordsTier_expectException() {
    ResponseEntity<String> response = testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/records-tiers",
            String.class, 100);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertTrue(response.getBody().contains("Provided dataset id: [100] is not valid. "));

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

  private String removeCreationDate(String body) {
    JSONObject jsonObject = new JSONObject(body);
    jsonObject.remove("creation-date");
    return jsonObject.toString();
  }

  int extractDatasetId(String value) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode node = objectMapper.readTree(value);
      return node.get("dataset-id").asInt();
    } catch (JsonProcessingException e) {
      return -1;
    }
  }

  String removeRecordIdFields(String body){
    ObjectMapper mapper = new ObjectMapper();
    try{
      JsonNode rootNode = mapper.readTree(body);
      JsonNode firstElem = rootNode.get(0);
      JsonNode secondElem = rootNode.get(1);
      ((ObjectNode) firstElem).remove("record-id");
      ((ObjectNode) secondElem).remove("record-id");
      ((ArrayNode) rootNode).set(0, firstElem);
      ((ArrayNode) rootNode).set(1, secondElem);
      return rootNode.toPrettyString();
    } catch (JsonProcessingException e){
      return body;
    }
  }

  String expectedContentTierJson(String actual, String recordId) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode rootNode = mapper.readTree(actual);
      JsonNode tierNode = rootNode.get("recordTierCalculationSummary");
      ((ObjectNode) tierNode).remove("europeanaRecordId");
      ((ObjectNode) tierNode).put("europeanaRecordId", recordId);
      ((ObjectNode) rootNode).set("recordTierCalculationSummary", tierNode);
      return rootNode.toPrettyString();
    } catch (JsonProcessingException e) {
      return actual;
    }
  }
}
