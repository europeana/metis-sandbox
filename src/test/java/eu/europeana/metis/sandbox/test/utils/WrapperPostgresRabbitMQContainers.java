package eu.europeana.metis.sandbox.test.utils;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

public class WrapperPostgresRabbitMQContainers {
    //TODO: Not reading the annotations, needs to fix it
    public PostgreSQLContainer<?> postgresContainer = PostgresContainerInitializerIT.postgreSQLContainer;
    public RabbitMQContainer rabbitmqContainer = RabbitMQContainerInitializerIT.rabbitMQContainer;
}
