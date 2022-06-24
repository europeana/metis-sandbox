package eu.europeana.metis.sandbox.test.utils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * This class is meant to be extended from integration unit test classes that require an underlying database.
 * <p>The container will be started and reused in all tests. Make sure that the tests are independent and purge any data that
 * were inserted from each test at the end of the test. The annotations are propagated to all the classes that extend this class.
 * Furthermore the @Sql annotation will be executed for each test.</p>
 */
//We drop and re-create the db
@Sql(scripts = {"classpath:database/schema_problem_patterns_drop.sql", "classpath:database/schema_problem_patterns.sql"})
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=none",//We do not want hibernate creating the db
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL9Dialect", "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@SuppressWarnings("resource")
public class PostgresContainerInitializerIT {

  static final PostgreSQLContainer<?> postgreSQLContainer;
  public static final String POSTGRES_VERSION = "postgres:9.6";

  static {
    postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_VERSION)
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
