package eu.europeana.metis.sandbox.test.utils;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

@TestConfiguration
public class PostgresTestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final PostgreSQLContainer<?> postgreSQLContainer;
  public static final String POSTGRES_VERSION = "postgres:14-alpine";

  static {
    postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_VERSION);
    postgreSQLContainer.start();
    dynamicProperties();
    logConfiguration();
  }

  private static void logConfiguration() {
    LOGGER.info("Postgres container created:");
    LOGGER.info("jdbcUrl: {}", postgreSQLContainer.getJdbcUrl());

    if (!postgreSQLContainer.getUsername().isBlank() && !postgreSQLContainer.getPassword().isBlank()) {
      LOGGER.info("Username and password were loaded");
    }
  }

  private static void dynamicProperties() {
    System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl());
    System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername());
    System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword());
    System.setProperty("spring.jpa.hibernate.ddl-auto", "none");
    System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
  }

  public static void dynamicProperty(String key, Function<PostgreSQLContainer<?>, String> getValue){
    System.setProperty(key, getValue.apply(postgreSQLContainer));
  }

  public static void runScripts(List<String> sqlScriptsInOrder) {
    var containerDelegate = new JdbcDatabaseDelegate(postgreSQLContainer, "");
    sqlScriptsInOrder.forEach(sqlScript -> ScriptUtils.runInitScript(containerDelegate, sqlScript));
  }
}
