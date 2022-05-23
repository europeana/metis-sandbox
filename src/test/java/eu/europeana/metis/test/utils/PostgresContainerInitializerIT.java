package eu.europeana.metis.test.utils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * This class is meant to be extended from integration unit test classes that require an underlying database.
 * <p>The container will be started and reused in all tests. Make sure that the tests are independent and purge any data that were
 * inserted from each test at the end of the test.
 * The annotations are propagated to all the classes that extend this class.</p>
 */
@Sql("classpath:database/schema_problem_patterns.sql") //We want the sql script to create the db
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=none",//We do not want hibernate creating the db
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL9Dialect"
})
@SuppressWarnings("resource")
public class PostgresContainerInitializerIT {

  static final PostgreSQLContainer<?> postgreSQLContainer;

  static {
    postgreSQLContainer = new PostgreSQLContainer<>("postgres:9.6")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
    postgreSQLContainer.start();
  }


  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
  }

  private void close() {
    //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
  }

}
