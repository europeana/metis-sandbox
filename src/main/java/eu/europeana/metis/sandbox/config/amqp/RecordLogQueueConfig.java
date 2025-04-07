package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Config for record log. This binds an exchange with a queue using a routing key that listens for all messages traveling through
 * the message broker
 */
@Configuration
class RecordLogQueueConfig extends QueueConsumerConfig {

    @Value("${sandbox.rabbitmq.queues.record.log.concurrency}")
    private int concurrentConsumers;

    @Value("${sandbox.rabbitmq.queues.record.log.max-concurrency}")
    private int maxConsumers;

    @Value("${sandbox.rabbitmq.queues.record.log.prefetch}")
    private int messagePrefetchCount;

    /**
     * Instantiates a new Record log queue configuration.
     *
     * @param messageConverter  the message converter
     * @param amqpConfiguration the amqp configuration
     */
    public RecordLogQueueConfig(MessageConverter messageConverter) {
        super(messageConverter);
    }
    /**
     * Record log factory simple rabbit listener container factory.
     *
     * @param configurer        the configurer
     * @param connectionFactory the connection factory
     * @return the simple rabbit listener container factory
     */
    @Bean
    SimpleRabbitListenerContainerFactory recordLogFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory, concurrentConsumers, maxConsumers, messagePrefetchCount);
    }
}
