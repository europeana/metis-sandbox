package eu.europeana.metis.sandbox.test.utils;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * This class is meant to be used directly without extending from integration unit test classes that require an underlying
 * database.
 * <p>An approach is to use the {@link DynamicPropertySource} annotation and then pass the registry to the method
 * {@link #dynamicProperties(DynamicPropertyRegistry)}</p>
 * <p>The container will be started and reused in all tests. Make sure that the tests are independent and purge any data that
 * were inserted from each test at the end of the test. </p>
 */
@SuppressWarnings("resource")
public class PostgresContainerInitializerIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresContainerInitializerIT.class);
  static final PostgreSQLContainer<?> postgreSQLContainer;
  public static final String POSTGRES_VERSION = "postgres:9.6";

  static {
    //Enable configuration reuse to avoid the warning message in the console, even though the re-usability works without it.
    TestcontainersConfiguration.getInstance().updateUserConfig("testcontainers.reuse.enable", "true");
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

  public static void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

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

  /**
   * Runs the sql scripts provided in order.
   * <p>This method can be used instead of the {@link org.springframework.test.context.jdbc.Sql} annotation.
   * A reason to use this method is to have a better control of when the scripts are executed. For example if an insertion of data
   * happens during beans creations.</p>
   *
   * @param sqlScriptsInOrder the scripts to run in order
   */
  public static void runScripts(List<String> sqlScriptsInOrder) {
    var containerDelegate = new JdbcDatabaseDelegate(postgreSQLContainer, "");
    sqlScriptsInOrder.forEach(sqlScript -> ScriptUtils.runInitScript(containerDelegate, sqlScript));

  }

  private void close() {
    //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
  }

}
