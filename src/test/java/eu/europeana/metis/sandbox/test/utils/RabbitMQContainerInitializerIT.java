package eu.europeana.metis.sandbox.test.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.test.context.SpringRabbitTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.Map;
@SpringRabbitTest
//@EnableRabbit
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RabbitMQContainerInitializerIT {

    public static final String RABBITMQ_VERSION = "rabbitmq:3.9.12-management";
    public static final String VIRTUAL_HOST = "/";
    public static final RabbitMQContainer rabbitMQContainer;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;

    static {
        rabbitMQContainer = new RabbitMQContainer(RABBITMQ_VERSION)
                .withVhost(VIRTUAL_HOST);
//                .withExchange("sandbox", "direct")
//                .withExchange("sandbox.dlq", "direct")
//                .withQueue("sandbox.record.log")
//                .withQueue("sandbox.record.log.dlq")
//                .withBinding("sandbox", "sandbox.record.log",
//                        Map.of("consumers", "2", "max-consumers", "2", "prefetch", "1"),
//                        "sandbox.record.#", "queue")
//                .withBinding("sandbox.dlq", "sandbox.record.log.dlq",
//                        Map.of("consumers", "2", "max-consumers", "2", "prefetch", "1"),
//                        "sandbox.record.#", "queue")
//                .withQueue("sandbox.record.created")
//                .withQueue("sandbox.record.created.dlq")
//                .withBinding("sandbox", "sandbox.record.created")
//                .withBinding("sandbox.dlq", "sandbox.record.created.dlq")
//                .withQueue("sandbox.record.transformation.edm.external")
//                .withQueue("sandbox.record.transformation.edm.external.dlq")
//                .withBinding("sandbox", "sandbox.record.transformation.edm.external")
//                .withBinding("sandbox.dlq", "sandbox.record.transformation.edm.external.dlq")
//                .withQueue("sandbox.record.ordered")
//                .withQueue("sandbox.record.ordered.dlq")
//                .withBinding("sandbox", "sandbox.record.ordered")
//                .withBinding("sandbox.dlq", "sandbox.record.ordered.dlq")
//                .withQueue("sandbox.record.validated.external")
//                .withQueue("sandbox.record.validated.external.dlq")
//                .withBinding("sandbox", "sandbox.record.validated.external")
//                .withBinding("sandbox.dlq", "sandbox.record.validated.external.dlq")
//                .withQueue("sandbox.record.validated.internal")
//                .withQueue("sandbox.record.validated.internal.dlq")
//                .withBinding("sandbox", "sandbox.record.validated.internal")
//                .withBinding("sandbox.dlq", "sandbox.record.validated.internal.dlq")
//                .withQueue("sandbox.record.transformed")
//                .withQueue("sandbox.record.transformed.dlq")
//                .withBinding("sandbox", "sandbox.record.transformed")
//                .withBinding("sandbox.dlq", "sandbox.record.transformed.dlq")
//                .withQueue("sandbox.record.normalized")
//                .withQueue("sandbox.record.normalized.dlq")
//                .withBinding("sandbox", "sandbox.record.normalized")
//                .withBinding("sandbox.dlq", "sandbox.record.normalized.dlq")
//                .withQueue("sandbox.record.enriched")
//                .withQueue("sandbox.record.enriched.dlq")
//                .withBinding("sandbox", "sandbox.record.enriched")
//                .withBinding("sandbox.dlq", "sandbox.record.enriched.dlq")
//                .withQueue("sandbox.record.media.processed")
//                .withQueue("sandbox.record.media.processed.dlq")
//                .withBinding("sandbox", "sandbox.record.media.processed")
//                .withBinding("sandbox.dlq", "sandbox.record.media.processed.dlq")
//                .withQueue("sandbox.record.published")
//                .withQueue("sandbox.record.published.dlq")
//                .withBinding("sandbox", "sandbox.record.published")
//                .withBinding("sandbox.dlq", "sandbox.record.published.dlq");
        rabbitMQContainer.start();

    }

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry){
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.virtual-host", () -> VIRTUAL_HOST);
        System.out.println("I got here");
    }

    private void close() {
        //We do not close the container, Ryuk will handle closing at the end of all the unit tests.
    }

    @Test
    void testing(){

        System.out.println(rabbitMQContainer.isRunning());

    }

}
