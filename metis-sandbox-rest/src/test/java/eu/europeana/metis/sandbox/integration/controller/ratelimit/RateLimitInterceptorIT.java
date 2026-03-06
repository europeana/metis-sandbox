package eu.europeana.metis.sandbox.integration.controller.ratelimit;


import static eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor.X_RATE_LIMIT_LIMIT;
import static eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor.X_RATE_LIMIT_REMAINING;
import static eu.europeana.metis.sandbox.controller.ratelimit.RateLimitInterceptor.X_RATE_LIMIT_RESET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

  private final RateLimitInterceptor rateLimitInterceptor;

  @Autowired
  public RateLimitInterceptorIT(RateLimitInterceptor rateLimitInterceptor) {
    this.rateLimitInterceptor = rateLimitInterceptor;
  }

  @Test
  void preHandle() throws Exception {
    String ipAddress = "192.168.1.2";
    String uri = "/record/validation";

    int maxRequests = 20;
    for (int i = 1; i <= maxRequests + 1; i++) {
      PreHandleResult preHandleResult = doPreHandle(ipAddress, uri);
      if (i <= maxRequests) {
        // These requests should be allowed
        assertEquals(String.valueOf(maxRequests), preHandleResult.response().getHeader(X_RATE_LIMIT_LIMIT));
        assertEquals(String.valueOf(20 - i), preHandleResult.response().getHeader(X_RATE_LIMIT_REMAINING));
        assertTrue(Integer.parseInt(Objects.requireNonNull(preHandleResult.response().getHeader(X_RATE_LIMIT_RESET))) <= 3600);
        assertTrue(preHandleResult.result);
      } else {
        // Final request should be blocked
        assertEquals(String.valueOf(maxRequests), preHandleResult.response().getHeader(X_RATE_LIMIT_LIMIT));
        assertNull(preHandleResult.response().getHeader(X_RATE_LIMIT_REMAINING));
        assertTrue(Integer.parseInt(Objects.requireNonNull(preHandleResult.response().getHeader(X_RATE_LIMIT_RESET))) <= 3600);
        assertFalse(preHandleResult.result);
      }
    }
  }

  private PreHandleResult doPreHandle(String ipAddress, String uri) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(ipAddress);
    request.setRequestURI(uri);

    MockHttpServletResponse response = new MockHttpServletResponse();
    boolean preHandleResult = rateLimitInterceptor.preHandle(request, response, new Object());

    return new PreHandleResult(preHandleResult, response);
  }

  private record PreHandleResult(boolean result, MockHttpServletResponse response) {

  }
}
