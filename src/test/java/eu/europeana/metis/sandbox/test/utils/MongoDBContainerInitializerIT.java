package eu.europeana.metis.sandbox.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;

public class MongoDBContainerInitializerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBContainerInitializerIT.class);
    static final MongoDBContainer mongoDBContainer;
    public static final String MONGO_VERSION = "mongo:5.0.12";
//    public static final int MONGO_PORT = 49180;

    static {
        mongoDBContainer = new MongoDBContainer(MONGO_VERSION);
        mongoDBContainer.start();

        logConfiguration();
    }

    private static void logConfiguration() {
        LOGGER.info("MongoDB container created:");
        LOGGER.info("Url: {}", mongoDBContainer.getReplicaSetUrl());
        LOGGER.info("Host: {}", mongoDBContainer.getHost());
        LOGGER.info("Port: {}", mongoDBContainer.getFirstMappedPort());
        LOGGER.info("Container Info: {}", mongoDBContainer.getContainerInfo());

    }

    public static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.url", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.db", () -> "test");
        registry.add("spring.data.mongodb.username", () -> "admin");
        registry.add("spring.data.mongodb.password", () -> "admin");

    }

    private void close() {
        //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
    }

}
