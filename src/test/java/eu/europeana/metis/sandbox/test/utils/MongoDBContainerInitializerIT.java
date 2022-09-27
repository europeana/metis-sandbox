package eu.europeana.metis.sandbox.test.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;

public class MongoDBContainerInitializerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBContainerInitializerIT.class);
    private static final MongoDBContainer mongoDBContainer;
    public static final String MONGO_VERSION = "mongo:5.0.12";

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

    }

    public static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.port", mongoDBContainer::getFirstMappedPort);
        registry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        registry.add("spring.data.mongodb.db", () -> "test");

        // TODO: 13/09/2022 We should perhaps remove the specifics here and use the default spring configuration properties
        //Sandbox specific datasource properties
        registry.add("sandbox.publish.mongo.application-name", () -> "mongo-testcontainer-test");
        registry.add("sandbox.publish.mongo.db", () -> "test");
        registry.add("sandbox.publish.mongo.hosts", mongoDBContainer::getHost);
        registry.add("sandbox.publish.mongo.ports", mongoDBContainer::getFirstMappedPort);

    }

    private void close() {
        //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
    }

}
