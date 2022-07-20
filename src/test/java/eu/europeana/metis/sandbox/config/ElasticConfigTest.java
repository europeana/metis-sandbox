package eu.europeana.metis.sandbox.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europeana.metis.sandbox.SandboxApplication;
import eu.europeana.metis.sandbox.test.utils.PostgresContainerInitializerIT;
import eu.europeana.metis.sandbox.test.utils.RabbitMQContainerInitializerIT;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = SandboxApplication.class)
class ElasticConfigTest {

  @DynamicPropertySource
  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    PostgresContainerInitializerIT.dynamicProperties(registry);
    PostgresContainerInitializerIT.runScripts(List.of(
        "database/schema_drop.sql", "database/schema.sql",
        "database/schema_problem_patterns_drop.sql", "database/schema_problem_patterns.sql"));
    RabbitMQContainerInitializerIT.properties(registry);
  }

  @BeforeEach
  void cleanUpPostgres() {
    PostgresContainerInitializerIT.runScripts(List.of("database/schema_drop.sql", "database/schema.sql"));
  }

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
