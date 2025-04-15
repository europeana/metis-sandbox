package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;

/**
 * The type Queue consumer configuration for workflow listeners.
 * Every listener has a {@link SimpleRabbitListenerContainerFactory}.
 * <br /> If changes like increasing concurrency for a listener are needed,
 * every subclass can be customized by using the SimpleRabbitListenerContainerFactory.
 */
public class QueueConsumerConfig {
    private final MessageConverter messageConverter;

    /**
     * Instantiates a new Queue consumer configuration.
     *
     * @param messageConverter the message converter
     */
    protected QueueConsumerConfig(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    /**
     * Gets simple rabbit listener container factory.
     *
     * @param configurer                  the configurer
     * @param connectionFactory           the connection factory
     * @param concurrentQueueConsumers    the concurrent queue consumers
     * @param maxConcurrentQueueConsumers the maximum concurrent queue consumers
     * @param messagePrefetchCount        the message prefetch count
     * @return the simple rabbit listener container factory
     */
    protected SimpleRabbitListenerContainerFactory getSimpleRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory,
            Integer concurrentQueueConsumers, Integer maxConcurrentQueueConsumers, Integer messagePrefetchCount) {
        var factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setConcurrentConsumers(concurrentQueueConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentQueueConsumers);
        factory.setPrefetchCount(messagePrefetchCount);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
