package eu.europeana.metis.sandbox.integration.controller.ratelimit;


import static eu.europeana.metis.sandbox.test.utils.S3TestContainersConfiguration.BUCKET_NAME;
import static eu.europeana.metis.sandbox.test.utils.SolrTestContainersConfiguration.SOLR_COLLECTION_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.test.utils.MongoTestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.RabbitMQTestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.S3TestContainersConfiguration;
import eu.europeana.metis.sandbox.test.utils.SolrTestContainersConfiguration;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SandboxApplication.class)
@Import({PostgresTestContainersConfiguration.class, RabbitMQTestContainersConfiguration.class,
    MongoTestContainersConfiguration.class, SolrTestContainersConfiguration.class, S3TestContainersConfiguration.class})
public class RateLimitInterceptorIT {

    @Autowired
    private ControllerEndpointHandlerMapping mapping;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeAll
    static void beforeAll() {
        //Sandbox specific properties
        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.jdbcUrl", PostgreSQLContainer::getJdbcUrl);
        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.username", PostgreSQLContainer::getUsername);
        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.password", PostgreSQLContainer::getPassword);
        PostgresTestContainersConfiguration.dynamicProperty("sandbox.datasource.driverClassName", container -> "org.postgresql.Driver");

        PostgresTestContainersConfiguration.runScripts(List.of(
            "database/schema_drop.sql", "database/schema.sql",
            "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql",
            "database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql",
            "database/schema_validation_drop.sql", "database/schema_validation.sql"
        ));

        //Sandbox specific datasource properties
        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.application-name", container -> "mongo-testcontainer-test");
        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.db", container -> "test");
        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.hosts", MongoDBContainer::getHost);
        MongoTestContainersConfiguration.dynamicProperty("sandbox.publish.mongo.ports", container -> container.getFirstMappedPort().toString());

        //Sandbox specific datasource properties
        SolrTestContainersConfiguration.dynamicProperty("sandbox.publish.solr.hosts",
            container -> "http://" + container.getHost() + ":" + container.getSolrPort() + "/solr/" + SOLR_COLLECTION_NAME);

        //Sandbox specific datasource properties
        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.access-key", LocalStackContainer::getAccessKey);
        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.secret-key", LocalStackContainer::getSecretKey);
        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.endpoint", container -> container.getEndpointOverride(S3).toString());
        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.signing-region", LocalStackContainer::getRegion);
        S3TestContainersConfiguration.dynamicProperty("sandbox.s3.thumbnails-bucket", container -> BUCKET_NAME);
    }

    @Test
    void preHandle_idHashFunction_expectSuccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.2");
        request.setRequestURI("/record/validation");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitInterceptor.preHandle(request, response, mapping);

        assertEquals("20", response.getHeader("X-Rate-Limit-Limit"));
        assertEquals("19", response.getHeader("X-Rate-Limit-Remaining"));
        assertEquals("3600", response.getHeader("X-Rate-Limit-Reset"));

        MockHttpServletResponse anotherResponse = new MockHttpServletResponse();
        rateLimitInterceptor.preHandle(request, anotherResponse, mapping);

        assertEquals("20", anotherResponse.getHeader("X-Rate-Limit-Limit"));
        assertEquals("18", anotherResponse.getHeader("X-Rate-Limit-Remaining"));
        assertTrue(Integer.parseInt(Objects.requireNonNull(anotherResponse.getHeader("X-Rate-Limit-Reset"))) < 3600);
    }
}
