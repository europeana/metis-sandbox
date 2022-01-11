package eu.europeana.metis.sandbox;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled until fixed, the url initialization of the default xslt is not working if not an http!")
@SpringBootTest
class SandboxApplicationIT {

  // Suppress: Tests should include assertions
  // This test is ensuring the application context loads with no exceptions
  @SuppressWarnings("squid:S2699")
  @Test
  void contextLoads() {
  }

}
