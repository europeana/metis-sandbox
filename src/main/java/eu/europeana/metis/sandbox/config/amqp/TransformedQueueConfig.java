package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * The type Transformed queue configuration.
 */
@Configuration
public class TransformedQueueConfig extends AbstractQueueConsumerConfig {

    @Value("${sandbox.rabbitmq.queues.record.transformed.concurrency}")
    private int concurrentConsumers;

    @Value("${sandbox.rabbitmq.queues.record.transformed.max-concurrency}")
    private int maxConsumers;

    @Value("${sandbox.rabbitmq.queues.record.transformed.prefetch}")
    private int messagePrefetchCount;

    /**
     * Instantiates a new Transformed queue configuration.
     *
     * @param messageConverter the message converter
     */
    public TransformedQueueConfig(MessageConverter messageConverter) {
        super(messageConverter);
    }

    /**
     * Internal validation factory simple rabbit listener container factory.
     *
     * @param configurer        the configurer
     * @param connectionFactory the connection factory
     * @return the simple rabbit listener container factory
     */
    @Bean
    SimpleRabbitListenerContainerFactory internalValidationFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory, concurrentConsumers, maxConsumers, messagePrefetchCount);
    }
}
