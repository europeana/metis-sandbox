package eu.europeana.metis.sandbox.service.problempatterns;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DirtiesContext
public class PostgresContainerInitializer {

//  @ClassRule
  @Container
  public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:9.6")
      .withDatabaseName("test")
      .withUsername("test")
      .withPassword("test");


  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url",postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
  }

}
