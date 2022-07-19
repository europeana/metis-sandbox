package eu.europeana.metis.sandbox.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.common.exception.ElasticConfigurationException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ElasticConfigTest {

  @Test
  void getConfig() {
    ElasticConfig.setConfigurationResource("application.yml");
    Map<String, String> map = ElasticConfig.loadAndGetConfig();
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

  @Test
  void getConfig_Error() {
    ElasticConfig.setConfigurationResource("temp.yml");
    assertThrows(ElasticConfigurationException.class, () -> ElasticConfig.loadAndGetConfig());
  }

  @Test
  void loadAttacher() {
    ElasticConfig.setConfigurationResource("application.yml");
    assertTrue(ElasticConfig.loadAttacher());
  }

  @Test
  void loadAttacher_Error() {
    ElasticConfig.setConfigurationResource("temp.yml");
    assertFalse(ElasticConfig.loadAttacher());
  }
}
