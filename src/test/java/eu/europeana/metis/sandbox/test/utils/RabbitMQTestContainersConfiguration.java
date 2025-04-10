package eu.europeana.metis.sandbox.test.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.RabbitMQContainer;

@TestConfiguration
public class RabbitMQTestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  //Use the *-management versions that contain the rabbitmqadmin cli command, otherwise the commands will fail.
  public static final String RABBITMQ_VERSION = "rabbitmq:3.9.12-management-alpine";
  public static final String VIRTUAL_HOST = "testVhost";
  private static final RabbitMQContainer rabbitMQContainer;

  static {
    rabbitMQContainer = new RabbitMQContainer(RABBITMQ_VERSION).withVhost(VIRTUAL_HOST);
    rabbitMQContainer.start();
    dynamicProperties();
    logConfiguration();
  }

  public static void logConfiguration() {
    LOGGER.info("Rabbitmq container created:");
    LOGGER.info("Host: {}", rabbitMQContainer.getHost());
    LOGGER.info("Amqp Port: {}", rabbitMQContainer.getAmqpPort());
    LOGGER.info("Http Port: {}", rabbitMQContainer.getHttpPort());
    try {
      final ExecResult execResult = rabbitMQContainer.execInContainer("rabbitmqctl", "list_vhosts");
      if (execResult.getStdout().contains(VIRTUAL_HOST)) {
        LOGGER.info("Virtual host: {}", VIRTUAL_HOST);
      } else {
        throw new RuntimeException("Virtual host not found in container");
      }
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }

    if (!rabbitMQContainer.getAdminUsername().isBlank() && !rabbitMQContainer.getAdminPassword().isBlank()) {
      LOGGER.info("Admin username and password were loaded");
    }
  }

  private static void dynamicProperties() {
    System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
    System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getAmqpPort().toString());
    System.setProperty("spring.rabbitmq.username", rabbitMQContainer.getAdminUsername());
    System.setProperty("spring.rabbitmq.password", rabbitMQContainer.getAdminPassword());
    System.setProperty("spring.rabbitmq.virtual-host", VIRTUAL_HOST);
  }
}
