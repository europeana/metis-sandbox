package eu.europeana.metis.sandbox.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.awaitility.Awaitility;
import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.test.utils.TestContainer;
import eu.europeana.metis.sandbox.test.utils.TestContainerFactoryIT;
import eu.europeana.metis.sandbox.test.utils.TestContainerType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static eu.europeana.metis.sandbox.common.locale.Country.ITALY;
import static eu.europeana.metis.sandbox.common.locale.Language.IT;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SandboxApplication.class)
@TestPropertySource(locations = "/application.yml",
        properties = {"sandbox.dataset.clean.frequency= 0 */5 * * * ?",
        "sandbox.dataset.clean.days-to-preserve=-1"})
class DatasetRemovalIT {

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
    void datasetsAreRemoved_expectSuccess(){
        FileSystemResource dataset = new FileSystemResource(
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + "zip" +
                        File.separator + "dataset-valid-small.zip");

        ResponseEntity<String> response = makeHarvestingByFile(dataset, null);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().matches("\\{\"dataset-id\":\"\\d\"\\}"));
        final int expectedDatasetId = extractDatasetId(response.getBody());
        assertTrue(expectedDatasetId > 0);

        // Give time for the full harvesting to happen
        Awaitility.await().atMost(10, MINUTES)
                .until(() -> Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress",
                        String.class, expectedDatasetId).getBody()).contains("COMPLETED"));

        ResponseEntity<String> getDatasetResponse =
                testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress", String.class, expectedDatasetId);
        assertEquals(HttpStatus.OK, getDatasetResponse.getStatusCode());
        assertNotNull(getDatasetResponse.getBody());
        assertTrue(getDatasetResponse.getBody().contains("\"total-records\":2"));

        // Give time for the data removal to happen
        Awaitility.await().atMost(10, MINUTES)
                .until(() -> Objects.requireNonNull(testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress",
                        String.class, expectedDatasetId).getBody()).contains("Provided dataset id: [" + expectedDatasetId + "] " +
                        "is not valid. "));

        ResponseEntity<String> getDatasetErrorResponse =
                testRestTemplate.getForEntity(getBaseUrl() + "/dataset/{id}/progress", String.class, expectedDatasetId);
        assertEquals(HttpStatus.BAD_REQUEST, getDatasetErrorResponse.getStatusCode());
        assertNotNull(getDatasetErrorResponse.getBody());
        assertTrue(getDatasetErrorResponse.getBody().contains("Provided dataset id: [" + expectedDatasetId +"] " +
                "is not valid. "));

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

    int extractDatasetId(String value) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(value);
            return node.get("dataset-id").asInt();
        } catch (JsonProcessingException e) {
            return -1;
        }
    }
}
