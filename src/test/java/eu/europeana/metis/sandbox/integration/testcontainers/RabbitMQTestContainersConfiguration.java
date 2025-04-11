package eu.europeana.metis.sandbox.integration.testcontainers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Provides {@link TestConfiguration} RabbitMQ Testcontainers.
 * <p>
 * This class it meant to be executed during integration tests which would initialize a single static containers to be used for
 * multiple tests. To use this, {@link Import} it in test classes.
 * <p>
 * Notice: do not change the static nature of the components unless there is an explicit requirement for a container per test.
 */
@TestConfiguration
public class RabbitMQTestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  //Use the *-management versions that contain the rabbitmqadmin cli command, otherwise the commands will fail.
  public static final String RABBITMQ_VERSION = "rabbitmq:3.9.12-management-alpine";
  private static final RabbitMQContainer rabbitMQContainer;

  static {
    rabbitMQContainer = new RabbitMQContainer(RABBITMQ_VERSION);
    rabbitMQContainer.start();
    setDynamicProperties();
    logConfiguration();
  }

  private static void setDynamicProperties() {
    System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
    System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getAmqpPort().toString());
    System.setProperty("spring.rabbitmq.username", rabbitMQContainer.getAdminUsername());
    System.setProperty("spring.rabbitmq.password", rabbitMQContainer.getAdminPassword());
    System.setProperty("spring.rabbitmq.virtual-host", "/");
  }

  private static void logConfiguration() {
    LOGGER.info("Rabbitmq container created:");
    LOGGER.info("Host: {}", rabbitMQContainer.getHost());
    LOGGER.info("Amqp Port: {}", rabbitMQContainer.getAmqpPort());
    LOGGER.info("Http Port: {}", rabbitMQContainer.getHttpPort());
    try {
      ExecResult result = rabbitMQContainer.execInContainer("rabbitmqctl", "list_vhosts");
      LOGGER.info("Available vhosts: {}", result.getStdout());
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to retrieve vhosts", e);
    }
  }
  public static void configureCustomVHost(String vhost) {
    createVHost(vhost);
    System.setProperty("spring.rabbitmq.virtual-host", vhost);
  }

  private static void createVHost(String vhost) {
    try {
      rabbitMQContainer.execInContainer("rabbitmqctl", "add_vhost", vhost);
      rabbitMQContainer.execInContainer(
          "rabbitmqctl", "set_permissions", "-p", vhost, rabbitMQContainer.getAdminUsername(), ".*", ".*", ".*");
      LOGGER.info("Created and configured vhost: {}", vhost);
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException("Failed to create vhost: " + vhost, e);
    }
  }
}
