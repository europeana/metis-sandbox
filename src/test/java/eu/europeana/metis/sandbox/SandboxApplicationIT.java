package eu.europeana.metis.sandbox;


import eu.europeana.metis.sandbox.test.utils.WrapperPostgresRabbitMQContainers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
// Nested class for configuration
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = SandboxApplication.class)
@ActiveProfiles("integration-test-profile")
@Configuration
class SandboxApplicationIT extends WrapperPostgresRabbitMQContainers {
  // Suppress: Tests should include assertions
  // This test is ensuring the application context loads with no exceptions
  @SuppressWarnings("squid:S2699")
  @Test
  void contextLoads() {
    System.out.println("This works?");
  }

  @Configuration
  public static class InnerConfig{



  }

}
