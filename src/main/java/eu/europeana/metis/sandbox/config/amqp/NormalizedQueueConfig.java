package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * The type Normalized queue configuration.
 */
@Configuration
public class NormalizedQueueConfig extends QueueConsumerConfig {

    @Value("${sandbox.rabbitmq.queues.record.normalized.concurrency}")
    private int concurrentConsumers;

    @Value("${sandbox.rabbitmq.queues.record.normalized.max-concurrency}")
    private int maxConsumers;

    @Value("${sandbox.rabbitmq.queues.record.normalized.prefetch}")
    private int messagePrefetchCount;

    /**
     * Instantiates a new Normalized queue configuration.
     *
     * @param messageConverter the message converter
     */
    public NormalizedQueueConfig(MessageConverter messageConverter) {
        super(messageConverter);
    }

    @Bean
    SimpleRabbitListenerContainerFactory enrichmentFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory, concurrentConsumers, maxConsumers, messagePrefetchCount);
    }
}
