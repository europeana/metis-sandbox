package eu.europeana.metis.sandbox.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.europeana.metis.sandbox.integration.testcontainers.MongoTestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.PostgresTestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.S3TestContainersConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.SandboxIntegrationConfiguration;
import eu.europeana.metis.sandbox.integration.testcontainers.SolrTestContainersConfiguration;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import({
    PostgresTestContainersConfiguration.class,
    MongoTestContainersConfiguration.class,
    SolrTestContainersConfiguration.class,
    S3TestContainersConfiguration.class
})
class SandboxApplicationIT {
  // TODO: 28/06/2022 Github actions seems to download the docker image everytime, check if there is a cache that we can apply.

  @BeforeAll
  static void beforeAll() {
    SandboxIntegrationConfiguration.testContainersConfiguration();
  }

  @Test
  void contextLoads(ApplicationContext applicationContext) {
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean(DataSource.class));
  }
}
