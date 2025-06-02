package eu.europeana.metis.sandbox.integration.controller;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import eu.europeana.metis.sandbox.integration.testcontainers.MongoTestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.SandboxIntegrationConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.SolrTestContainersConfiguration;
import eu.europeana.metis.security.test.JwtUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
@Import({
    PostgresTestContainersConfiguration.class,
    MongoTestContainersConfiguration.class,
    SolrTestContainersConfiguration.class,
    S3TestContainersConfiguration.class
})
class DatasetControllerIT {

  private static final Path ZIP_DIR = Path.of("src", "test", "resources", "zip");
  private static final FileSystemResource VALID_ZIP = new FileSystemResource(
      ZIP_DIR.resolve("dataset-valid-with-xslt-file-small.zip"));
  private static final FileSystemResource VALID_XSLT = new FileSystemResource(
      ZIP_DIR.resolve("xslt-file-for-transformation-to-edm-external-test.xslt"));
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @MockBean
  JwtDecoder jwtDecoder;

  private final JwtUtils jwtUtils;
  private final TestRestTemplate testRestTemplate = new TestRestTemplate();

  @LocalServerPort
  private int port;

  public DatasetControllerIT() {
    jwtUtils = new JwtUtils(List.of());
  }

  @BeforeAll
  static void beforeAll() {
    SandboxIntegrationConfiguration.testContainersConfiguration();
  }

  @BeforeEach
  void setup() {
    when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
  }

  @Test
  void harvestDatasetFromFile() throws JsonProcessingException {
    ResponseEntity<String> response = triggerHarvestByFile();

    assertAll(
        () -> assertEquals(HttpStatus.ACCEPTED, response.getStatusCode()),
        () -> assertNotNull(response.getBody())
    );

    int datasetId = extractDatasetId(response.getBody());
    assertTrue(datasetId > 0);
    awaitDatasetCompletion(datasetId);
  }

  @Test
  void harvestDatasetFromURL() throws JsonProcessingException {
    ResponseEntity<String> response = triggerHarvestFromUrl();

    assertAll(
        () -> assertEquals(HttpStatus.ACCEPTED, response.getStatusCode()),
        () -> assertNotNull(response.getBody())
    );

    int datasetId = extractDatasetId(response.getBody());
    assertTrue(datasetId > 0);
    awaitDatasetCompletion(datasetId);
  }

  @Test
  void harvestDatasetOaiPmh() throws JsonProcessingException, URISyntaxException {
    ResponseEntity<String> response = triggerHarvestingByOAIPMH();

    assertAll(
        () -> assertEquals(HttpStatus.ACCEPTED, response.getStatusCode()),
        () -> assertNotNull(response.getBody())
    );

    int datasetId = extractDatasetId(response.getBody());
    assertTrue(datasetId > 0);
    awaitDatasetCompletion(datasetId);
  }

  @Test
  void getAllCountries() {
    ResponseEntity<String> response = testRestTemplate.getForEntity(getBaseUrl() + "/dataset/countries", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    Country.getCountryListSortedByName().forEach(
        country -> assertTrue(response.getBody().contains(country.xmlValue()))
    );
  }

  @Test
  void getAllLanguages() {
    ResponseEntity<String> response = testRestTemplate.getForEntity(getBaseUrl() + "/dataset/languages", String.class);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());

    Language.getLanguageListSortedByName().forEach(
        language -> assertTrue(response.getBody().contains(language.xmlValue()))
    );
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(MOCK_VALID_TOKEN);
    return headers;
  }

  private String getBaseUrl() {
    return "http://localhost:" + port;
  }

  private void awaitDatasetCompletion(int datasetId) {
    Awaitility.await()
              .atMost(2, MINUTES)
              .until(() -> {
                String body = testRestTemplate
                    .getForEntity(getBaseUrl() + "/dataset/{id}/progress", String.class, datasetId)
                    .getBody();
                return body != null && body.contains("COMPLETED");
              });
  }

  private ResponseEntity<String> triggerHarvestByFile() {
    HttpHeaders headers = createHeaders();
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("dataset", VALID_ZIP);
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());
    body.add("xsltFile", VALID_XSLT);

    return testRestTemplate.postForEntity(
        getBaseUrl() + "/dataset/{name}/harvestByFile",
        new HttpEntity<>(body, headers), String.class, "testDataset");
  }

  private ResponseEntity<String> triggerHarvestFromUrl() {
    HttpHeaders headers = createHeaders();
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("url", VALID_ZIP.getFile().toURI().toString());
    body.add("xsltFile", VALID_XSLT);
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    return testRestTemplate.postForEntity(
        getBaseUrl() + "/dataset/{name}/harvestByUrl",
        new HttpEntity<>(body, headers), String.class, "testDataset");
  }

  private ResponseEntity<String> triggerHarvestingByOAIPMH() throws URISyntaxException {
    HttpHeaders headers = createHeaders();
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("url", new URI("https://metis-repository-rest.test.eanadev.org/repository/oai").toString());
    body.add("setspec", "oai_integration_test");
    body.add("metadataformat", "edm");
    body.add("country", ITALY.xmlValue());
    body.add("language", IT.xmlValue());

    return testRestTemplate.postForEntity(
        getBaseUrl() + "/dataset/{name}/harvestOaiPmh",
        new HttpEntity<>(body, headers), String.class, "testDataset");
  }

  private int extractDatasetId(String body) throws JsonProcessingException {
    JsonNode node = OBJECT_MAPPER.readTree(body);
    return node.get("dataset-id").asInt();
  }
}
