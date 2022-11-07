package eu.europeana.metis.sandbox.test.utils;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQContainerIT extends TestContainer {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQContainerIT.class);
  //Use the *-management versions that contain the rabbitmqadmin cli command, otherwise the commands will fail.
  public static final String RABBITMQ_VERSION = "rabbitmq:3.9.12-management-alpine";
  public static final String VIRTUAL_HOST = "testVhost";
  private static RabbitMQContainer rabbitMQContainer;

  public RabbitMQContainerIT() {
    rabbitMQContainer = new RabbitMQContainer(RABBITMQ_VERSION)
        .withVhost(VIRTUAL_HOST);
    rabbitMQContainer.start();

    logConfiguration();
  }
  @Override
  public void logConfiguration() {
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

  @Override
  public void dynamicProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
    registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    registry.add("spring.rabbitmq.virtual-host", VIRTUAL_HOST::toString);
  }
}
