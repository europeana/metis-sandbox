//package eu.europeana.metis.sandbox.integration.controller;
//
//import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
//import static eu.europeana.metis.sandbox.common.locale.Language.IT;
//import static eu.europeana.metis.sandbox.test.utils.S3TestContainersConfiguration.BUCKET_NAME;
//import static eu.europeana.metis.sandbox.test.utils.SolrTestContainersConfiguration.SOLR_COLLECTION_NAME;
//import static eu.europeana.metis.security.test.JwtUtils.MOCK_VALID_TOKEN;
//import static java.util.concurrent.TimeUnit.MINUTES;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.mockito.Mockito.when;
//import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import eu.europeana.metis.sandbox.SandboxApplication;
//import eu.europeana.metis.sandbox.test.utils.MongoTestContainersConfiguration;
//import eu.europeana.metis.sandbox.test.utils.PostgresTestContainersConfiguration;
//import eu.europeana.metis.sandbox.test.utils.RabbitMQTestContainersConfiguration;
//import eu.europeana.metis.sandbox.test.utils.S3TestContainersConfiguration;
//import eu.europeana.metis.sandbox.test.utils.SolrTestContainersConfiguration;
//import eu.europeana.metis.security.test.JwtUtils;
//import java.io.File;
//import java.util.List;
//import java.util.Objects;
//import org.awaitility.Awaitility;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.context.annotation.Import;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.oauth2.jwt.JwtDecoder;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.testcontainers.containers.MongoDBContainer;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.containers.localstack.LocalStackContainer;
//
//@SpringBootTest(
//        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//        classes = SandboxApplication.class)
//@TestPropertySource(locations = "/application.yml",
//    properties = {"sandbox.dataset.clean.frequency= */30 * * * * ?",
//        "sandbox.dataset.clean.days-to-preserve=-1"})
//@Import({PostgresTestContainersConfiguration.class, RabbitMQTestContainersConfiguration.class,
//    MongoTestContainersConfiguration.class, SolrTestContainersConfiguration.class, S3TestContainersConfiguration.class})
//class DatasetRemovalIT {
//
//    private final TestRestTemplate testRestTemplate = new TestRestTemplate();
//
//    @MockBean
//    JwtDecoder jwtDecoder;
//
//    @LocalServerPort
//    private int port;
//
//    private final JwtUtils jwtUtils;
//
//    public DatasetRemovalIT() {
//        jwtUtils = new JwtUtils(List.of());
//    }
//
//    @BeforeAll
//    static void beforeAll() {
//        //Sandbox specific properties
//        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.jdbcUrl", PostgreSQLContainer::getJdbcUrl);
//        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.username", PostgreSQLContainer::getUsername);
//        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.password", PostgreSQLContainer::getPassword);
//        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.driverClassName", container -> "org.postgresql.Driver");
//
//        PostgresTestContainersConfiguration.runScripts(List.of(
//            "database/schema_drop_except_transform_xslt.sql", "database/schema.sql",
//            "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql",
//            "database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql"
//        ));
//
//        //Sandbox specific datasource properties
//        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.application-name", container -> "mongo-testcontainer-test");
//        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.db", container -> "test");
//        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.hosts", MongoDBContainer::getHost);
//        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.ports", container -> container.getFirstMappedPort().toString());
//
//        //Sandbox specific datasource properties
//        SolrTestContainersConfiguration.dynamicProperty("sandbox.publish.solr.hosts",
//            container -> "http://" + container.getHost() + ":" + container.getSolrPort() + "/solr/" + SOLR_COLLECTION_NAME);
//
//        //Sandbox specific datasource properties
//        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.access-key", LocalStackContainer::getAccessKey);
//        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.secret-key", LocalStackContainer::getSecretKey);
//        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.endpoint", container -> container.getEndpointOverride(S3).toString());
//        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.signing-region", LocalStackContainer::getRegion);
//        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.thumbnails-bucket", container -> BUCKET_NAME);
//    }
//
//    private String getBaseUrl() {
//        return "http://localhost:" + port;
//    }
//
//    @Test
//    void datasetsAreRemoved_expectSuccess(){
//        FileSystemResource dataset = new FileSystemResource(
//                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
//                        File.separator + "dataset-valid-small.zip");
//
//        ResponseEntity<String> response = makeHarvestingByFile(dataset, null);
//        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertTrue(response.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
//        final int expectedDatasetId = extractDatasetId(response.getBody());
//        assertTrue(expectedDatasetId > 0);
//
//        // Give time for the full harvesting to happen
//        Awaitility.await().atMost(10, MINUTES)
//                  .until(() -> Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress",
//                        String.class, expectedDatasetId).getBody()).contains("COMPLETED"));
//
//        ResponseEntity<String> getDatasetResponse =
//                testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress", String.class, expectedDatasetId);
//        assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
//        assertNotNull(getDatasetResponse.getBody());
//        assertTrue(getDatasetResponse.getBody().contains("\"total-records\":2"));
//
//        // Give time for the data removal to happen
//        Awaitility.await().atMost(10, MINUTES)
//                .until(() -> Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress",
//                        String.class, expectedDatasetId).getBody()).contains("Provided dataset id: [" + expectedDatasetId + "] " +
//                        "is not valid. "));
//
//        ResponseEntity<String> getDatasetErrorResponse =
//                testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress", String.class, expectedDatasetId);
//        assertEquals(HttpStatus.BAD_REQUEST, getDatasetErrorResponse.getStatusCode());
//        assertNotNull(getDatasetErrorResponse.getBody());
//        assertTrue(getDatasetErrorResponse.getBody().contains("Provided dataset id: [" + expectedDatasetId +"] " +
//                "is not valid. "));
//
//    }
//
//    private ResponseEntity<String> makeHarvestingByFile(FileSystemResource dataset, FileSystemResource xsltFile) {
//        when(jwtDecoder.decode(MOCK_VALID_TOKEN)).thenReturn(jwtUtils.getEmptyRoleJwt());
//
//        HttpHeaders requestHeaders = new HttpHeaders();
//        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
//        requestHeaders.setBearerAuth(MOCK_VALID_TOKEN);
//        MultiValueMap<String, Object> body
//                = new LinkedMultiValueMap<>();
//        body.add("dataset", dataset);
//        body.add("country", ITALY.xmlValue());
//        body.add("language", IT.xmlValue());
//
//        if (xsltFile != null) {
//            body.add("xsltFile", xsltFile);
//        }
//
//        return testRestTemplate.postForEntity(getBaseUrl() + "/dataset/{name}/harvestByFile",
//                new HttpEntity<>(body, requestHeaders), String.class, "testDataset");
//    }
//
//    int extractDatasetId(String value) {
//        try {
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode node = objectMapper.readTree(value);
//            return node.get("dataset-id").asInt();
//        } catch (JsonProcessingException e) {
//            return -1;
//        }
//    }
//}
