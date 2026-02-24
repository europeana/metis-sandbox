package eu.europeana.metis.sandbox.integration.testcontainers;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Provides {@link TestConfiguration} MongoDB Testcontainers.
 * <p>
 * This class it meant to be executed during integration tests which would initialize a single static containers to be used for
 * multiple tests. To use this, {@link Import} it in test classes.
 * <p>
 * Notice: do not change the static nature of the components unless there is an explicit requirement for a container per test.
 */
@TestConfiguration
public class MongoTestContainersConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String MONGO_VERSION = "mongo:5.0.12";
  private static final MongoDBContainer mongoDBContainer;

  static {
    mongoDBContainer = new MongoDBContainer(MONGO_VERSION);
    mongoDBContainer.start();
    setDynamicProperties();
    logConfiguration();
  }

  private static void setDynamicProperties() {
    System.setProperty("spring.data.mongodb.uri", mongoDBContainer.getReplicaSetUrl());
    System.setProperty("spring.data.mongodb.host", mongoDBContainer.getHost());
    System.setProperty("spring.data.mongodb.port", mongoDBContainer.getFirstMappedPort().toString());
    System.setProperty("spring.data.mongodb.db", "test");
  }

  public static void setDynamicProperty(String key, Function<MongoDBContainer, String> getValue) {
    System.setProperty(key, getValue.apply(mongoDBContainer));
  }

  private static void logConfiguration() {
    LOGGER.info("MongoDB container created:");
    LOGGER.info("Url: {}", mongoDBContainer.getReplicaSetUrl());
    LOGGER.info("Host: {}", mongoDBContainer.getHost());
    LOGGER.info("Port: {}", mongoDBContainer.getFirstMappedPort());
  }
}
