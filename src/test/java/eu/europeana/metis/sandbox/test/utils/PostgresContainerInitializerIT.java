package eu.europeana.metis.sandbox.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * This class is meant to be extended or used directly without extending from integration unit test classes that require an
 * underlying database.
 * <p>The container will be started and reused in all tests. Make sure that the tests are independent and purge any data that
 * were inserted from each test at the end of the test. </p>
 */
@SuppressWarnings("resource")
public class PostgresContainerInitializerIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresContainerInitializerIT.class);
  static final PostgreSQLContainer<?> postgreSQLContainer;
  public static final String POSTGRES_VERSION = "postgres:9.6";

  static {
    postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_VERSION)
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);
    postgreSQLContainer.start();

    logConfiguration();
  }

  private static void logConfiguration() {
    LOGGER.info("Postgres container created:");
    LOGGER.info("jdbcUrl: {}", postgreSQLContainer.getJdbcUrl());
    LOGGER.info("Username: {}", postgreSQLContainer.getUsername());
    LOGGER.info("Password: {}", postgreSQLContainer.getPassword());
  }

  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

    //Default settings. If we want to change behavior on specific test cases we should remove this block from here
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQL9Dialect");
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

    // TODO: 27/06/2022 We should perhaps remove the specifics here and use the default spring configuration properties
    //Sandbox specific datasource properties
    registry.add("sandbox.datasource.jdbcUrl", postgreSQLContainer::getJdbcUrl);
    registry.add("sandbox.datasource.username", postgreSQLContainer::getUsername);
    registry.add("sandbox.datasource.password", postgreSQLContainer::getPassword);
    registry.add("sandbox.datasource.driverClassName", () -> "org.postgresql.Driver");
  }

  private void close() {
    //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
  }

}
