package eu.europeana.metis.sandbox.integration.testcontainers;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

/**
 * Provides {@link TestConfiguration} Postgres Testcontainers.
 * <p>
 * This class it meant to be executed during integration tests which would initialize a single static containers to be used for
 * multiple tests. To use this, {@link Import} it in test classes.
 * <p>
 * Notice: do not change the static nature of the components unless there is an explicit requirement for a container per test.
 */
@TestConfiguration
public class PostgresTestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String POSTGRES_VERSION = "postgres:14-alpine";
  private static final PostgreSQLContainer<?> postgreSQLContainer;

  static {
    postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_VERSION);
    postgreSQLContainer.start();
    setDynamicProperties();
    logConfiguration();
  }

  private static void setDynamicProperties() {
    System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
    System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
    System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());
    System.setProperty("spring.jpa.hibernate.ddl-auto", "none");
    System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
  }

  public static void setDynamicProperty(String key, Function<PostgreSQLContainer<?>, String> getValue){
    System.setProperty(key, getValue.apply(postgreSQLContainer));
  }

  private static void logConfiguration() {
    LOGGER.info("Postgres container created:");
    LOGGER.info("jdbcUrl: {}", postgreSQLContainer.getJdbcUrl());
    LOGGER.info("Username: {}", postgreSQLContainer.getUsername());
    LOGGER.info("Password: {}", postgreSQLContainer.getPassword());

    if (!postgreSQLContainer.getUsername().isBlank() && !postgreSQLContainer.getPassword().isBlank()) {
      LOGGER.info("Username and password were loaded");
    }
  }

  public static void runScripts(List<String> sqlScriptsInOrder) {
    JdbcDatabaseDelegate containerDelegate = new JdbcDatabaseDelegate(postgreSQLContainer, "");
    sqlScriptsInOrder.forEach(sqlScript -> ScriptUtils.runInitScript(containerDelegate, sqlScript));
  }
}
