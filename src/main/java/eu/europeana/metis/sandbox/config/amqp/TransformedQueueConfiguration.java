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
public class TransformedQueueConfiguration extends QueueConsumerConfiguration {

  @Value("${sandbox.rabbitmq.queues.record.transformed.consumers:12}")
  private int concurrentConsumers;

  @Value("${sandbox.rabbitmq.queues.record.transformed.max-consumers:12}")
  private int maxConsumers;

  @Value("${sandbox.rabbitmq.queues.record.transformed.prefetch:3}")
  private int messagePrefetchCount;

  /**
   * Instantiates a new Transformed queue configuration.
   *
   * @param messageConverter the message converter
   */
  public TransformedQueueConfiguration(MessageConverter messageConverter) {
    super(messageConverter);
  }

  /**
   * Internal validation factory simple rabbit listener container factory.
   *
   * @param configurer the configurer
   * @param connectionFactory the connection factory
   * @return the simple rabbit listener container factory
   */
  @Bean
  SimpleRabbitListenerContainerFactory internalValidationFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer,
      ConnectionFactory connectionFactory) {
    super.setConcurrentQueueConsumers(concurrentConsumers);
    super.setMaxConcurrentQueueConsumers(maxConsumers);
    super.setMessagePrefetchCount(messagePrefetchCount);
    return super.getSimpleRabbitListenerContainerFactory(configurer, connectionFactory);
  }
}
