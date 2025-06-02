package eu.europeana.metis.sandbox.integration.controller.ratelimit;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.integration.testcontainers.MongoTestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.SandboxIntegrationConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.SolrTestContainersConfiguration;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.servlet.ControllerEndpointHandlerMapping;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = SandboxApplication.class)
@Import({
    PostgresTestContainersConfiguration.class,
    MongoTestContainersConfiguration.class,
    SolrTestContainersConfiguration.class,
    S3TestContainersConfiguration.class
})
class RateLimitInterceptorIT {

    @Autowired
    private ControllerEndpointHandlerMapping mapping;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeAll
    static void beforeAll() {
        SandboxIntegrationConfiguration.testContainersConfiguration();
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
