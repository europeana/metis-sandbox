package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * The type Validated internal queue configuration.
 */
@Configuration
public class ValidatedInternalQueueConfig extends QueueConsumerConfig {

    @Value("${sandbox.rabbitmq.queues.record.validated.internal.concurrency}")
    private int concurrentConsumers;

    @Value("${sandbox.rabbitmq.queues.record.validated.internal.max-concurrency}")
    private int maxConsumers;

    @Value("${sandbox.rabbitmq.queues.record.validated.internal.prefetch}")
    private int messagePrefetchCount;


    /**
     * Instantiates a new Validated internal queue configuration.
     *
     * @param messageConverter the message converter
     */
    public ValidatedInternalQueueConfig(MessageConverter messageConverter) {
        super(messageConverter);
    }


    /**
     * Normalization factory simple rabbit listener container factory.
     *
     * @param configurer        the configurer
     * @param connectionFactory the connection factory
     * @return the simple rabbit listener container factory
     */
    @Bean
    SimpleRabbitListenerContainerFactory normalizationFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory, concurrentConsumers, maxConsumers, messagePrefetchCount);
    }
}
