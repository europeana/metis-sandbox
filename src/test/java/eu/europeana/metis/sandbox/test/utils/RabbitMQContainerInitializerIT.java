package eu.europeana.metis.sandbox.test.utils;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQContainerInitializerIT {

  //Use the *-management versions that contain the rabbitmqadmin cli command, otherwise the commands will fail.
  public static final String RABBITMQ_VERSION = "rabbitmq:3.9.12-management";
  public static final String VIRTUAL_HOST = "testVhost";
  public static final RabbitMQContainer rabbitMQContainer;
  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQContainerInitializerIT.class);

  static {
    rabbitMQContainer = new RabbitMQContainer(RABBITMQ_VERSION)
        .withVhost(VIRTUAL_HOST)
        //        .withExchange("testExchange", "topic")
        //        .withQueue("testQueue")
        //        .withBinding("testExchange", "testQueue", new HashMap<>(), "testQueue", "queue")
        .withReuse(true);

    rabbitMQContainer.start();
  }

  public RabbitMQContainerInitializerIT() {
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
    LOGGER.info("Admin username: {}", rabbitMQContainer.getAdminUsername());
    LOGGER.info("Admin password: {}", rabbitMQContainer.getAdminPassword());
  }

  @DynamicPropertySource
  public static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
    registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
    registry.add("spring.rabbitmq.virtual-host", VIRTUAL_HOST::toString);
  }

  private void close() {
    //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
  }

}
