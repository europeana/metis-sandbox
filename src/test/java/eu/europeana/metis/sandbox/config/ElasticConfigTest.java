package eu.europeana.metis.sandbox.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test the configuration for elastic apm agent warning: it will display a message that the Elastic APM server is not available
 * (Connection Refused) unless you configure a http://localhost:8200 (see properties file)
 */
@SpringBootTest(classes = ElasticConfig.class)
@EnableConfigurationProperties
class ElasticConfigTest {

  @Autowired
  ElasticConfig elasticConfig;

  @Test
  void getConfig() {
    Map<String, String> map = elasticConfig.getApm();

    assertTrue(map.containsKey("service_name"));
    assertTrue(map.containsKey("application_packages"));
    assertTrue(map.containsKey("server_url"));
    assertTrue(map.containsKey("capture_body"));
    assertTrue(map.containsKey("capture_headers"));
    assertTrue(map.containsKey("metrics_interval"));
    assertTrue(map.containsKey("environment"));
    assertTrue(map.containsKey("hostname"));
    assertTrue(map.containsKey("enabled"));
    assertTrue(map.containsKey("recording"));
    assertTrue(map.containsKey("instrument"));
  }
}
