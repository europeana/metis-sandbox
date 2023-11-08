package eu.europeana.metis.sandbox.config.amqp;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The type Created queue configuration.
 */
@Configuration
public class CreatedQueueConfig extends QueueConsumerConfig {

  @Value("${sandbox.rabbitmq.queues.record.created.consumers}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.created.max-consumers}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.created.prefetch}")
  private int messagePrefetchCount;

  /**
   * Instantiates a new Queue consumer configuration.
   *
   * @param messageConverter the message converter
   */
  public CreatedQueueConfig(MessageConverter messageConverter) {
    super(messageConverter);
  }

  /**
   * External validation factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory externalValidationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    setConcurrentQueueConsumers(concurrentConsumers);
    setMaxConcurrentQueueConsumers(maxConsumers);
    setMessagePrefetchCount(messagePrefetchCount);
    return getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }
}
