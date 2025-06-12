package eu.europeana.metis.sandbox.integration.controller.ratelimit;


import static eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor.X_RATE_LIMIT_LIMIT;
import static eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor.X_RATE_LIMIT_REMAINING;
import static eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor.X_RATE_LIMIT_RESET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.config.RateLimitConfig;
import eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@SpringBootTest(classes = RateLimitConfig.class)
@EnableAutoConfiguration
@Import({PostgresTestContainersConfiguration.class})
class RateLimitInterceptorIT {

  private RateLimitInterceptor rateLimitInterceptor;

  @Autowired
  public RateLimitInterceptorIT(RateLimitInterceptor rateLimitInterceptor) {
    this.rateLimitInterceptor = rateLimitInterceptor;
  }

  @Test
  void preHandle() throws Exception {
    String ipAddress = "192.168.1.2";
    String uri = "/record/validation";

    MockHttpServletResponse response1 = doPreHandle(ipAddress, uri);
    assertEquals("20", response1.getHeader(X_RATE_LIMIT_LIMIT));
    assertEquals("19", response1.getHeader(X_RATE_LIMIT_REMAINING));
    assertTrue(Integer.parseInt(Objects.requireNonNull(response1.getHeader(X_RATE_LIMIT_RESET))) <= 3600);

    MockHttpServletResponse response2 = doPreHandle(ipAddress, uri);
    assertEquals("20", response2.getHeader(X_RATE_LIMIT_LIMIT));
    assertEquals("18", response2.getHeader(X_RATE_LIMIT_REMAINING));
    assertTrue(Integer.parseInt(Objects.requireNonNull(response2.getHeader(X_RATE_LIMIT_RESET))) <= 3600);

    MockHttpServletResponse response3 = doPreHandle(ipAddress, uri);
    assertEquals("20", response3.getHeader(X_RATE_LIMIT_LIMIT));
    assertEquals("17", response3.getHeader(X_RATE_LIMIT_REMAINING));
    assertTrue(Integer.parseInt(Objects.requireNonNull(response3.getHeader(X_RATE_LIMIT_RESET))) <= 3600);
  }

  private MockHttpServletResponse doPreHandle(String ipAddress, String uri) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(ipAddress);
    request.setRequestURI(uri);

    MockHttpServletResponse response = new MockHttpServletResponse();
    rateLimitInterceptor.preHandle(request, response, new Object());

    return response;
  }
}
