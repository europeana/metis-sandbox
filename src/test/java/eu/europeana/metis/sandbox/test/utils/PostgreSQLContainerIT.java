package eu.europeana.metis.sandbox.test.utils;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

public class PostgreSQLContainer extends TestContainerIT {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLContainer.class);

  private static org.testcontainers.containers.PostgreSQLContainer<?> postgreSQLContainer;

  public PostgreSQLContainer(String version) {
    postgreSQLContainer = new org.testcontainers.containers.PostgreSQLContainer<>(version)
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    postgreSQLContainer.start();

    logConfiguration();
  }

  @Override
  public void logConfiguration() {
    LOGGER.info("Postgres container created:");
    LOGGER.info("jdbcUrl: {}", postgreSQLContainer.getJdbcUrl());

    if (!postgreSQLContainer.getUsername().isBlank() && !postgreSQLContainer.getPassword().isBlank()) {
      LOGGER.info("Username and password were loaded");
    }
  }

  @Override
  public void dynamicProperties(DynamicPropertyRegistry registry) {
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
  public void runScripts(List<String> sqlScriptsInOrder) {
    var containerDelegate = new JdbcDatabaseDelegate(postgreSQLContainer, "");
    sqlScriptsInOrder.forEach(sqlScript -> ScriptUtils.runInitScript(containerDelegate, sqlScript));

  }
}
