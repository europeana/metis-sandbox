package eu.europeana.metis.sandbox.integration.testcontainers;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.MongoDBContainer;

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

  public static void setDynamicProperty(String key, Function<MongoDBContainer, String> getValue){
    System.setProperty(key, getValue.apply(mongoDBContainer));
  }

  private static void logConfiguration() {
    LOGGER.info("MongoDB container created:");
    LOGGER.info("Url: {}", mongoDBContainer.getReplicaSetUrl());
    LOGGER.info("Host: {}", mongoDBContainer.getHost());
    LOGGER.info("Port: {}", mongoDBContainer.getFirstMappedPort());
  }
}
