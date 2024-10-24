package eu.europeana.metis.sandbox.controller.ratelimit;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.test.utils.TestContainer;
import eu.europeana.metis.sandbox.test.utils.TestContainerFactoryIT;
import eu.europeana.metis.sandbox.test.utils.TestContainerType;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
        (webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                classes = SandboxApplication.class)
public class RateLimitInterceptorIT {

    @Autowired
    private ControllerEndpointHandlerMapping mapping;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @DynamicPropertySource
    public static void dynamicProperties(DynamicPropertyRegistry registry) {
        TestContainer postgresql = TestContainerFactoryIT.getContainer(TestContainerType.POSTGRES);
        postgresql.dynamicProperties(registry);
        postgresql.runScripts(List.of("database/schema_drop_except_transform_xslt.sql", "database/schema.sql"));
        postgresql.runScripts(List.of("database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql"));
        postgresql.runScripts(List.of("database/schema_lockrepository_drop.sql", "database/schema_lockrepository.sql"));
        postgresql.runScripts(List.of("database/schema_validation_drop.sql", "database/schema_validation.sql"));
        TestContainer rabbitMQ = TestContainerFactoryIT.getContainer(TestContainerType.RABBITMQ);
        rabbitMQ.dynamicProperties(registry);
        TestContainer mongoDBContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.MONGO);
        mongoDBContainerIT.dynamicProperties(registry);
        TestContainer solrContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.SOLR);
        solrContainerIT.dynamicProperties(registry);
        TestContainer s3ContainerIT = TestContainerFactoryIT.getContainer(TestContainerType.S3);
        s3ContainerIT.dynamicProperties(registry);
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
